"""Validate locked pilot data, not chatbot behavior or Gemini answer quality."""
import copy
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / 'eval/t12/pilot-halong-v1'

def validate(documents, cases, scenarios):
    assert len(documents) == 5 and len({d['sourceId'] for d in documents}) == 5
    by_source = {d['sourceId']: d for d in documents}
    for doc in documents:
        assert doc['scopeType'] == 'HOTEL_SCOPED' and doc['hotelId'] == 4 and doc['version'] == 1
        assert hashlib.sha256(doc['content'].encode()).hexdigest() == doc['contentSha256']
    assert len(cases) == 20 and len({c['id'] for c in cases}) == 20
    statuses = [c['expectedStatus'] for c in cases]
    assert statuses.count('GROUNDED_ANSWER') == 10
    assert statuses.count('INSUFFICIENT_EVIDENCE') == 6 and statuses.count('OUT_OF_SCOPE') == 4
    for case in cases:
        assert case['hotelId'] == 4 and case['question'].strip()
        if case['expectedStatus'] == 'GROUNDED_ANSWER':
            assert case['sources'] and case['requiredFacts'] and case['iterations'] == 3
            text = '\n'.join(by_source[s]['content'] for s in case['sources'])
            assert all(fact in text for fact in case['requiredFacts'])
        else:
            assert case['sources'] == [] and case['requiredFacts'] == [] and case['iterations'] == 1
    assert len(scenarios) == 12 and len({s['id'] for s in scenarios}) == 12
    assert all(s['businessWriteCalls'] == 0 for s in scenarios)

def main():
    manifest = json.loads((PACK / 'manifest.json').read_text(encoding='utf-8'))
    for name, digest in manifest['fileSha256'].items():
        assert hashlib.sha256((PACK / name).read_bytes()).hexdigest() == digest, name
    for name, digest in manifest['protectedEvaluationSha256'].items():
        assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == digest, name
    docs, cases, scenarios = [json.loads((PACK / name).read_text(encoding='utf-8'))
                              for name in ['corpus.json', 'rag-cases.json', 'r1-scenarios.json']]
    validate(docs, cases, scenarios)
    # Ensure validation rejects tampering rather than merely parsing JSON.
    for change in range(5):
        d, c, s = copy.deepcopy((docs, cases, scenarios))
        if change == 0: d[0]['hotelId'] = 1
        if change == 1: d[0]['scopeType'] = 'CHAIN_PUBLIC'
        if change == 2: c[0]['requiredFacts'] = ['không tồn tại trong nguồn']
        if change == 3: c[-1]['sources'] = ['VP-HL-DINING']
        if change == 4: s[0]['businessWriteCalls'] = 1
        try:
            validate(d, c, s)
        except (AssertionError, KeyError):
            continue
        raise AssertionError('Tampered pack was accepted')
    print(json.dumps(dict(status='PASS_DATA_INTEGRITY_ONLY', documents=5, ragCases=20,
                          ragPlannedObservations=40, r1ScenarioSpecifications=12, rejectedTamperCases=5,
                          protectedFilesUnchanged=len(manifest['protectedEvaluationSha256']),
                          applicationCasesExecuted=0, geminiCalls=0)))

if __name__ == '__main__':
    main()
