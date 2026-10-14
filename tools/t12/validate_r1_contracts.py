"""Offline R1-00 artifact validator; does not invoke an assistant or rank rooms."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import sys
import unicodedata
from collections import Counter
from datetime import date
from decimal import Decimal
from pathlib import Path
from urllib.parse import unquote, urljoin, urldefrag

from jsonschema import Draft202012Validator, FormatChecker
from jsonschema.exceptions import ValidationError
from openapi_spec_validator import validate as validate_openapi
from referencing import Registry, Resource

ROOT = Path(__file__).resolve().parents[2]
DOC = ROOT / 'docs/t12/r1-00/v2.0.0-draft.1'
FIX = ROOT / 'backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1'
EVAL = ROOT / 'eval/t12/r1/v0.1.0-draft'
FORMAT = FormatChecker()


def pairs_unique(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f'Duplicate JSON key: {key}')
        result[key] = value
    return result


def read(path):
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=pairs_unique,
                      parse_float=Decimal)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def pointer(value, fragment):
    if not fragment:
        return value
    require(fragment.startswith('/'), f'Unsupported pointer: {fragment}')
    for token in fragment[1:].split('/'):
        token = unquote(token).replace('~1', '/').replace('~0', '~')
        value = value[int(token)] if isinstance(value, list) else value[token]
    return value


def walk(value):
    yield value
    if isinstance(value, dict):
        for child in value.values():
            yield from walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk(child)


def patch(value, mutation):
    """Apply a test-only JSON Patch without mutating a fixture on disk."""
    path, operation = mutation['path'], mutation['op']
    if not path:
        require(operation in ('add', 'replace'), 'Cannot remove fixture root')
        return copy.deepcopy(mutation['value'])
    parent_path, token = path.rsplit('/', 1)
    parent = pointer(value, parent_path)
    token = token.replace('~1', '/').replace('~0', '~')
    if isinstance(parent, list):
        index = len(parent) if token == '-' else int(token)
        if operation == 'remove':
            parent.pop(index)
        elif operation == 'add':
            require(0 <= index <= len(parent), 'Invalid patch index')
            parent.insert(index, copy.deepcopy(mutation['value']))
        else:
            parent[index] = copy.deepcopy(mutation['value'])
    elif operation == 'remove':
        del parent[token]
    else:
        if operation == 'replace':
            require(token in parent, 'Missing replace target')
        parent[token] = copy.deepcopy(mutation['value'])
    return value


def semantic_criteria(criteria):
    if 'checkInDate' in criteria and 'checkOutDate' in criteria:
        require(date.fromisoformat(criteria['checkOutDate']) >
                date.fromisoformat(criteria['checkInDate']), 'Checkout must follow checkin')
    if 'budget' in criteria:
        require(Decimal(criteria['budget']['amount']) > 0, 'Budget must be positive')


def semantic_response(response):
    result = response['result']
    semantic_criteria(result['criteria'])
    cards, cov = result['recommendations'], result['coverage']
    discovered, evaluated, failed = (cov[k] for k in ('discoveredCount', 'evaluatedCount', 'failedCount'))
    require(0 <= failed <= evaluated <= discovered, 'Invalid coverage counts')
    require(cov['remainingCount'] == discovered - evaluated, 'Wrong remainingCount')
    require(cov['verifiedMatchCount'] <= evaluated - failed, 'Wrong verifiedMatchCount')
    if cov['status'] == 'COMPLETE':
        require(cov['catalogComplete'] and discovered == evaluated and failed == 0,
                'Incomplete search labelled COMPLETE')
        require(not cov['reasons'], 'COMPLETE must have no incomplete reasons')
    if cov['status'] == 'PARTIAL':
        require(cov['reasons'] and 'INCOMPLETE_COVERAGE' in result['warnings'],
                'Partial search needs a reason and warning')
    if cov['status'] == 'NOT_RUN':
        require(discovered == evaluated == failed == cov['remainingCount'] ==
                cov['verifiedMatchCount'] == 0 and not cov['catalogComplete'], 'NOT_RUN counts')
    if result['status'] in ('NEED_INFO', 'OUT_OF_SCOPE'):
        require(not cards and cov['status'] == 'NOT_RUN', 'Non-search response used candidates')
    if result['status'] == 'NO_MATCH':
        require(cov['status'] == 'COMPLETE' and cov['verifiedMatchCount'] == 0,
                'NO_MATCH must have complete coverage')
    require(cov['verifiedMatchCount'] >= len(cards), 'More cards than verified matches')
    require([c['rank'] for c in cards] == list(range(1, len(cards) + 1)), 'Non-contiguous ranks')
    require(len({c['room']['hotelRoomTypeId'] for c in cards}) == len(cards), 'Duplicate room IDs')
    require(len({c['recommendationId'] for c in cards}) == len(cards), 'Duplicate card IDs')
    if cards:
        require(result['criteria']['roomsCount'] == 1, 'Multi-room cards forbidden')
        require(not result['ambiguities'], 'Unresolved ambiguity in RESULTS')
    evidence_fields = [e['field'] for e in result['criteriaProvenance']]
    require(len(set(evidence_fields)) == len(evidence_fields), 'Duplicate provenance field')
    for evidence in result['criteriaProvenance']:
        value = result['criteria']
        for token in evidence['field'].split('.'):
            require(token in value, 'Provenance without criteria value')
            value = value[token]
    for card in cards:
        room, quote = card['room'], card['price']
        require(room['hotelRoomTypeId'] == quote['hotelRoomTypeId'], 'Quote room ID mismatch')
        expected_path = f"/portal/hotels/{room['hotelId']}/room-types/{room['hotelRoomTypeId']}"
        require(card['action']['path'] == expected_path, 'Detail path identity mismatch')
        require(card['stay'] == {k: result['criteria'][k] for k in
                                 ('checkInDate','checkOutDate','roomsCount')}, 'Card stay mismatch')
        nights = (date.fromisoformat(result['criteria']['checkOutDate']) -
                  date.fromisoformat(result['criteria']['checkInDate'])).days
        require(quote['totalNights'] == nights, 'Wrong quote nights')
        if quote['serviceFeeStatus'] == 'UNKNOWN':
            require(quote['serviceFeeAmount'] is None, 'Unknown fee is not zero')
            require(quote['chargeCoverage'] != 'ALL_MANDATORY_CHARGES', 'Unknown fee claimed all-in')
        budget = result['criteria'].get('budget')
        if budget is None:
            require(quote['budgetCheck'] == 'NOT_REQUESTED', 'Budget check without budget')
        elif budget['hard']:
            require(quote['budgetCheck'] == 'PASS', 'Hard budget must be verified')
        if quote['budgetCheck'] == 'PASS':
            require(budget is not None and quote['currency'] == budget['currency'], 'Budget currency mismatch')
            require(quote['chargeCoverage'] == budget['coverage'], 'Budget coverage mismatch')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--json-report', type=Path, help='Optional report path outside the dataset')
    args = parser.parse_args()
    counters = Counter()
    json_files = sorted([*DOC.rglob('*.json'), *FIX.rglob('*.json'), *EVAL.rglob('*.json')])
    documents = {p: read(p) for p in json_files}
    schemas = {p: doc for p, doc in documents.items() if p.name.endswith('.schema.json')}
    uri_docs = {p.as_uri(): doc for p, doc in documents.items()}
    for p, document in schemas.items():
        Draft202012Validator.check_schema(document)
        counters['schemas_validated'] += 1
        if '$id' in document:
            uri_docs[document['$id']] = document
    registry = Registry()
    for p, document in schemas.items():
        resource = Resource.from_contents(document)
        registry = registry.with_resource(p.as_uri(), resource)
        if '$id' in document:
            registry = registry.with_resource(document['$id'], resource)

    def validate(value, path, definition=None):
        schema = documents[path]
        if definition:
            schema = {'$ref': path.as_uri() + '#/$defs/' + definition}
        Draft202012Validator(schema, registry=registry, format_checker=FORMAT).validate(value)
        counters['instances_validated'] += 1

    for path, document in documents.items():
        for node in walk(document):
            if isinstance(node, dict) and '$ref' in node:
                base = document.get('$id', path.as_uri())
                target, fragment = urldefrag(urljoin(base, node['$ref']))
                require(target in uri_docs, f'Unresolved or network ref: {target}')
                pointer(uri_docs[target], fragment)
                counters['refs_resolved'] += 1

    # Bundle external schema refs in memory so OpenAPI validation is fully offline.
    advisor_path = DOC / 'schemas/advisor.schema.json'
    core_path = DOC / 'schemas/core.schema.json'
    bundled = copy.deepcopy(documents[DOC / 'openapi.json'])
    component_schemas = {}

    def rebase(value, namespace):
        if isinstance(value, dict):
            out = {}
            for key, item in value.items():
                if key in ('$id', '$schema'):
                    continue
                if key == '$ref' and item.startswith('#/$defs/'):
                    out[key] = '#/components/schemas/' + namespace + item.split('/')[-1]
                else:
                    out[key] = rebase(item, namespace)
            return out
        if isinstance(value, list):
            return [rebase(item, namespace) for item in value]
        return value

    for schema_path, namespace in [(advisor_path, 'Advisor_'), (core_path, 'Core_')]:
        for name, definition in documents[schema_path]['$defs'].items():
            component_schemas[namespace + name] = rebase(definition, namespace)
    for node in walk(bundled):
        if isinstance(node, dict) and '$ref' in node and node['$ref'].startswith('./schemas/'):
            namespace = 'Advisor_' if 'advisor.schema' in node['$ref'] else 'Core_'
            node['$ref'] = '#/components/schemas/' + namespace + node['$ref'].split('/')[-1]
    bundled['components']['schemas'] = component_schemas
    validate_openapi(bundled)
    counters['openapi_documents_validated'] = 1
    require(len(bundled['paths']) == 3, 'Only CHAT/SEARCH/REFRESH permitted')
    for item in bundled['paths'].values():
        require(list(item) == ['post'] and item['post']['x-business-write'] is False,
                'Unexpected API operation')

    for path in sorted((DOC / 'examples').glob('*.json')):
        definition = {'chat-request':'ChatRequest', 'search-request':'SearchRequest',
                      'extraction-proposal':'CriteriaExtractionProposal',
                      'refresh-request':'RefreshRequest', 'error-response':'ErrorResponse'}.get(path.stem, 'AdvisorResponse')
        value = documents[path]
        validate(value, advisor_path, definition)
        if definition == 'AdvisorResponse':
            semantic_response(value)
            counters['response_invariants_checked'] += 1

    manifest = documents[FIX / 'manifest.json']
    require(manifest['humanApproval'] == 'PENDING', 'Unexpected fixture approval')
    for entry in manifest['files']:
        path = FIX / entry['path']
        require(path in documents, f'Missing fixture: {path}')
        if 'schema' in entry:
            relative, fragment = entry['schema'].split('#')
            definition = fragment.split('/')[-1]
            for item in documents[path] if entry['arrayOf'] else [documents[path]]:
                validate(item, ROOT / relative, definition)

    prices = documents[FIX / 'pricing-responses-native.json']
    availability = documents[FIX / 'availability-responses-native.json']
    quotes = documents[FIX / 'quotes-normalized-proposed.json']
    pages = documents[FIX / 'discovery-pages-proposed.json']
    rooms = [room for page in pages for room in page['result']['content']]
    room_ids = {room['hotelRoomTypeId'] for room in rooms}
    require(len(rooms) == len(room_ids) == 26, 'Fixture catalog size/dedup mismatch')
    require(len({r['hotelId'] for r in rooms}) == 6, 'Fixture must cover sixth hotel')
    require({p['result']['hotelRoomTypeId'] for p in prices} == room_ids, 'Pricing fixture IDs')
    require({q['hotelRoomTypeId'] for q in quotes} == room_ids, 'Normalized fixture IDs')
    for index, (price, avail, quote) in enumerate(zip(prices, availability, quotes, strict=True)):
        price = price['result']
        nights = price['dailyPrices']
        require([n['date'] for n in nights] == ['2026-11-20','2026-11-21'], 'Native price nights')
        require(price['totalNights'] == len(nights), 'Native price night count')
        require(sum(Decimal(n['netAmount']) for n in nights) == Decimal(price['preTaxAmount']), 'Native pre-tax sum')
        require(Decimal(price['preTaxAmount']) + Decimal(price['vatAmount']) == Decimal(price['finalTotalAmount']), 'Native final sum')
        require(Decimal(quote['finalTotalAmount']) == Decimal(price['finalTotalAmount']), 'Normalized final mismatch')
        require(quote['serviceFeeStatus'] == 'UNKNOWN' and quote['serviceFeeAmount'] is None, 'Synthetic fee assumption')
        require([n['availabilityDate'] for n in avail['result']] == ['2026-11-20','2026-11-21'], 'Availability date mapping')
        require(all(n['hotelRoomTypeId'] == price['hotelRoomTypeId'] and n['availableCount'] ==
                    n['totalRooms'] - n['bookedRooms'] - n['lockedRooms'] - n['oooRooms'] for n in avail['result']), 'Availability fixture identity/counts')
        counters['core_fixture_triplets_checked'] += 1

    evaluation = documents[EVAL / 'cases.json']
    eval_manifest = documents[EVAL / 'manifest.json']
    validate(evaluation, EVAL / 'evaluation.schema.json')
    cases = evaluation['cases']
    require(eval_manifest['caseCount'] == len(cases), 'Evaluation count mismatch')
    require(len({c['id'] for c in cases}) == len(cases), 'Duplicate evaluation IDs')
    require({c['category'] for c in cases} == set(eval_manifest['requiredCategories']), 'Missing category')
    require(eval_manifest['humanApproval'] == 'PENDING' and not eval_manifest['goldenAnswersFrozen'], 'Golden answers must remain unapproved')
    for case in cases:
        require(ROOT / case['fixtureManifest'] == FIX / 'manifest.json', 'Fixture version mismatch')
        for turn in case['turns']:
            validate(turn['request'], advisor_path, turn['operation'].title() + 'Request')
        patched = {}
        for mutation in case['fixtureMutations']:
            path = ROOT / mutation['file']
            require(path in documents and path.is_relative_to(FIX), 'Mutation outside fixture bundle')
            value = patched.setdefault(path, copy.deepcopy(documents[path]))
            patched[path] = patch(value, mutation)
            counters['evaluation_patches_resolved'] += 1
        for key in ('expectedTopIds','eligibleIds','excludedIds','expectedRelativeOrder'):
            if key in case['expected']:
                require(set(case['expected'][key]).issubset(room_ids), f'Unknown expected IDs in {case["id"]}')
        if 'firstId' in case['expected']:
            require(case['expected']['firstId'] in room_ids, 'Unknown firstId')
        counters['evaluation_cases_checked'] += 1

    decisions = documents[DOC / 'decisions-and-approvals.json']
    decision_ids = {d['id'] for d in decisions['decisions']}
    require(set(decisions['integrationBlockers']).issubset(decision_ids), 'Unresolved approval blocker')
    require(decisions['contractApproval']['status'] == decisions['evaluationApproval']['status'] == 'PENDING', 'Approval drift')
    for path in decisions['governanceAlignment']['references']:
        require((ROOT / path).is_file(), 'Missing governance reference: ' + path)
        counters['governance_refs_resolved'] += 1
    coverage_mapping = documents[EVAL / 'governance-coverage.json']
    source = ROOT / coverage_mapping['source']
    require(hashlib.sha256(source.read_bytes()).hexdigest() == coverage_mapping['sourceSha256'],
            'Governance eval source changed; review mapping')
    mapped_ids = set()
    case_ids = {c['id'] for c in cases}
    source_text = source.read_text(encoding='utf-8')
    for row in coverage_mapping['requirements']:
        require(row['requirementId'] in source_text, 'Unknown governance requirement')
        require(row['requirementId'] not in mapped_ids, 'Duplicate requirement mapping')
        require(set(row['caseIds']).issubset(case_ids), 'Unknown evaluation case reference')
        mapped_ids.add(row['requirementId'])
    require(mapped_ids == {f'T12-V2-{i:03}' for i in range(1,36)}, 'R1 coverage mapping gap')
    counters['governance_requirements_mapped'] = len(mapped_ids)
    protected = documents[DOC / 'protected-user-files.json']
    for entry in protected['files']:
        require(hashlib.sha256((ROOT / entry['path']).read_bytes()).hexdigest().upper() ==
                entry['sha256'], 'Protected user file changed: ' + entry['path'])
        counters['protected_user_hashes_checked'] += 1
    evidence = documents[DOC / 'baseline-source-evidence.json']
    require(evidence['gitSha'] == decisions['baselineCommit'], 'Source baseline version mismatch')
    for entry in evidence['files']:
        path = ROOT / entry['path']
        require(path.is_file() and entry['baselineUnmodified'] is True, 'Missing source evidence')
        require(hashlib.sha256(path.read_bytes()).hexdigest() == entry['sha256'],
                'Source changed after audit: ' + entry['path'])
        counters['source_evidence_hashes_checked'] += 1

    # Reject security/price/card contradictions, not snapshots of implementation internals.
    def rejected(value, definition, label, semantic=False):
        try:
            validate(value, advisor_path, definition)
            if semantic:
                if definition == 'SearchRequest':
                    semantic_criteria(value['criteria'])
                else:
                    semantic_response(value)
        except (ValueError, ValidationError):
            # Schema ValidationError and semantic ValueError both count as rejection.
            counters['negative_cases_rejected'] += 1
            return
        raise ValueError('Negative example accepted: ' + label)

    chat = documents[DOC / 'examples/chat-request.json']
    search = documents[DOC / 'examples/search-request.json']
    positive = documents[DOC / 'examples/results-response.json']
    for field, value in [('ownerId','x'),('tenantId',7),('role','ADMIN'),('holdToken','x'),
                         ('criteriaProvenance',[])]:
        bad = dict(chat, **{field:value}); rejected(bad,'ChatRequest',field)
    for field, value in [('roomsCount',0),('adults',0),('children',-1),('checkInDate','2026-02-30')]:
        bad = copy.deepcopy(search);bad['criteria'][field]=value;rejected(bad,'SearchRequest',field)
    bad = copy.deepcopy(search);bad['criteria'].pop('locationKeyword');rejected(bad,'SearchRequest','missing location')
    bad = dict(chat, sessionId='00000000-0000-4000-8000-000000000001');rejected(bad,'ChatRequest','missing version')
    bad = copy.deepcopy(search);bad['criteria']['checkOutDate']='2026-11-19';rejected(bad,'SearchRequest','reversed dates',True)
    bad = copy.deepcopy(search);bad['criteria']['budget']={'amount':'0','currency':'VND','basis':'STAY_TOTAL','coverage':'BACKEND_QUOTED_TOTAL','hard':True};rejected(bad,'SearchRequest','zero budget',True)
    response_patches = [
        ('/result/recommendations/0/action/type','HOLD_ROOM',False),
        ('/result/recommendations/0/action/path','https://evil.example/checkout',False),
        ('/result/recommendations/0/action/path','/portal/checkout',False),
        ('/result/criteria/roomsCount',2,False),
        ('/result/recommendations/0/price/finalTotalAmount',880000,False),
        ('/result/recommendations/0/price/serviceFeeAmount','0',False),
        ('/result/recommendations/0/price/chargeCoverage','ALL_MANDATORY_CHARGES',False),
        ('/result/recommendations/0/price/hotelRoomTypeId',1001,True),
        ('/result/recommendations/0/action/path','/portal/hotels/1/room-types/1026',True),
        ('/result/recommendations/1/room/hotelRoomTypeId',1026,True),
        ('/result/recommendations/0/rank',2,True),
        ('/result/coverage/remainingCount',6,True),
        ('/result/coverage/failedCount',1,True),
        ('/result/recommendations/0/stay/checkOutDate','2026-11-23',True),
        ('/result/criteriaProvenance/1/field','locationKeyword',True),
    ]
    for path, value, semantic in response_patches:
        bad = patch(copy.deepcopy(positive), {'path':path,'op':'replace','value':value})
        rejected(bad,'AdvisorResponse',path,semantic)
    bad = copy.deepcopy(positive);bad['result']['criteria']['budget']={'amount':'900000','currency':'VND','basis':'STAY_TOTAL','coverage':'ALL_MANDATORY_CHARGES','hard':True};rejected(bad,'AdvisorResponse','unverified hard budget',True)
    bad = copy.deepcopy(documents[DOC / 'examples/incomplete-response.json']);bad['result']['status']='NO_MATCH';rejected(bad,'AdvisorResponse','partial no-match')
    bad = copy.deepcopy(positive);bad['result']['coverage']['status']='PARTIAL';bad['result']['coverage']['reasons']=['TIME_BUDGET'];rejected(bad,'AdvisorResponse','hidden partial',True)
    bad = copy.deepcopy(positive);bad['result']['ambiguities']=[{'field':'locationKeyword','reason':'MULTIPLE_LOCATIONS'}];rejected(bad,'AdvisorResponse','unresolved ambiguity')
    bad = copy.deepcopy(documents[DOC / 'examples/extraction-proposal.json']);bad['criteriaPatch']['role']='ADMIN';rejected(bad,'CriteriaExtractionProposal','untrusted model role')

    artifacts = sorted([*DOC.rglob('*'), *FIX.rglob('*'), *EVAL.rglob('*'), *(ROOT / 'tools/t12').glob('*')])
    for path in artifacts:
        if not path.is_file():
            continue
        text = path.read_text(encoding='utf-8')
        require(unicodedata.normalize('NFC', text) == text, f'NFD text: {path}')
        if path.suffix == '.md':
            for target in re.findall(r'\]\(([^)]+)\)', text):
                if target.startswith(('http:','https:','#')):
                    continue
                target = target.split('#')[0]
                require((path.parent / target).exists(), f'Broken Markdown link: {path}: {target}')
                counters['markdown_links_resolved'] += 1
    report = {'status':'PASS','scope':'ARTIFACT_VALIDATION_ONLY','engineEvaluation':'NOT_RUN',
              'counters':dict(counters),'caseCategories':dict(Counter(c['category'] for c in cases)),
              'evaluationCasesSha256':hashlib.sha256((EVAL / 'cases.json').read_bytes()).hexdigest(),
              'contractArtifactVersion':'2.0.0-draft.1','evaluationVersion':'0.1.0-draft'}
    if args.json_report:
        require(not args.json_report.resolve().is_relative_to(ROOT), 'Reports must be outside the repository')
        args.json_report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(f'FAIL: {type(error).__name__}: {error}', file=sys.stderr)
        sys.exit(1)
