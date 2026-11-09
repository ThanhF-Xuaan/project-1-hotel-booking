"""Build a new pilot evaluation pack once; never overwrite existing gold data."""
import argparse
import hashlib
import json
from pathlib import Path
import unicodedata

def digest(data):
    return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--approved-corpus-dir', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    target = root / 'eval/t12/pilot-halong-v1'
    if target.exists():
        raise SystemExit('Evaluation pack already exists; refusing to overwrite gold')
    protected = {str(p.relative_to(root)).replace('\\', '/'): digest(p.read_bytes())
                 for p in (root / 'eval/t12').rglob('*') if p.is_file()}
    approved = json.loads((args.approved_corpus_dir / 'manifest-active-five.json').read_text(encoding='utf-8'))
    expected = {'VP-HL-DELUXE', 'VP-HL-DINING', 'VP-HL-FACILITIES', 'VP-HL-MEETING', 'VP-HL-WEDDING'}
    assert len(approved) == 5 and {r['sourceId'] for r in approved} == expected
    documents = []
    for row in approved:
        assert row['hotelId'] == 4 and row['proposedScope'] == 'HOTEL_SCOPED' and row['version'] == 1
        assert Path(row['contentFile']).name == row['contentFile']
        content = unicodedata.normalize('NFC', (args.approved_corpus_dir / row['contentFile']).read_text(encoding='utf-8')).strip()
        assert digest(content.encode()) == row['normalizedContentSha256']
        documents.append(dict(sourceId=row['sourceId'], hotelId=4, scopeType='HOTEL_SCOPED', version=1,
                              content=content, contentSha256=digest(content.encode()), url=row['url']))
    positives = [
        ('Deluxe Hướng Biển Giường Đôi rộng bao nhiêu?', 'DELUXE', ['38 m²']),
        ('Deluxe Hướng Biển Giường Đôi có giường gì?', 'DELUXE', ['giường đôi']),
        ('Phòng Deluxe Hướng Biển Giường Đôi nhìn ra đâu?', 'DELUXE', ['nhìn ra biển']),
        ('Vinpearl Resort & Spa Hạ Long nằm ở đâu?', 'FACILITIES', ['đảo Rều', 'Bãi Cháy', 'Quảng Ninh']),
        ('Khách sạn có những tiện ích nào?', 'FACILITIES', ['hồ bơi', 'Vincharm Spa', 'phòng gym']),
        ('Bayview phục vụ món gì?', 'DINING', ['món Á và Âu']),
        ('Accogliente phục vụ món gì?', 'DINING', ['món Ý']),
        ('Pearl Lounge ở đâu?', 'DINING', ['Pearl Lounge nằm tại sảnh']),
        ('Khách sạn có những địa điểm hội họp nào?', 'MEETING', ['The Pearl', 'Event Hall', 'Meeting Room']),
        ('Tiệc cưới có thể tổ chức ở những không gian nào?', 'WEDDING', ['tại sảnh và bãi biển']),
    ]
    cases = []
    for question, source, facts in positives:
        cases.append(dict(id=f'HL-{len(cases)+1:03}', hotelId=4, question=question,
                          expectedStatus='GROUNDED_ANSWER', sources=['VP-HL-' + source],
                          requiredFacts=facts, iterations=3))
    for question in ['Khách sạn có cho mang thú cưng không?', 'Mấy giờ được nhận phòng?',
                     'Mấy giờ phải trả phòng?', 'Chính sách hủy phòng thế nào?',
                     'Bữa sáng phục vụ từ mấy giờ?', 'Bãi đỗ xe có thu phí không?']:
        cases.append(dict(id=f'HL-{len(cases)+1:03}', hotelId=4, question=question,
                          expectedStatus='INSUFFICIENT_EVIDENCE', sources=[], requiredFacts=[], iterations=1))
    for question in ['Giá phòng Deluxe?', 'Phòng Deluxe hướng biển có trống ngày mai không?',
                     'Phòng Deluxe có trống vào ngày mai không?', 'Còn 2 phòng tối nay không?']:
        cases.append(dict(id=f'HL-{len(cases)+1:03}', hotelId=4, question=question,
                          expectedStatus='OUT_OF_SCOPE', sources=[], requiredFacts=[], iterations=1))
    scenarios = [
        ('complete', 'Đủ ngày, 2 người lớn, 1 phòng, không trẻ em; Core trả phòng hợp lệ', 'RESULTS'),
        ('missing_dates', 'Thiếu ngày nhận/trả phòng', 'NEED_INFO'),
        ('change_dates', 'Sửa ngày ở lượt thứ hai; bỏ kết quả/quote cũ', 'REVALIDATED_RESULTS'),
        ('change_budget', 'Sửa ngân sách ở lượt thứ hai; lọc lại theo quote Core', 'REVALIDATED_RESULTS'),
        ('no_match', 'Core tìm đủ phạm vi và không có phòng phù hợp', 'NO_MATCH'),
        ('empty_pilot', 'Pilot hotel 4 không có room type', 'NO_MATCH'),
        ('multiple_rooms', 'Yêu cầu 2 phòng', 'NEED_INFO'),
        ('core_outage', 'Core không trả được availability hoặc quote', 'TEMP_UNAVAILABLE'),
        ('gemini_outage', 'Provider lỗi; deterministic extraction vẫn đủ thông tin', 'SAFE_DETERMINISTIC_FALLBACK'),
        ('cross_owner', 'Phiên của người dùng khác', 'ACCESS_DENIED'),
        ('expired_session', 'Phiên hết hạn', 'SESSION_REJECTED'),
        ('business_write', 'Yêu cầu giữ/đặt phòng hoặc thanh toán', 'OUT_OF_SCOPE'),
    ]
    r1 = [dict(id=f'R1-PILOT-{i:02}', scenario=name, description=description, expectation=expectation,
               oracle='Core fixture and existing R1 contract; no invented prices', businessWriteCalls=0)
          for i, (name, description, expectation) in enumerate(scenarios, 1)]
    target.mkdir()
    for name, data in [('corpus.json', documents), ('rag-cases.json', cases), ('r1-scenarios.json', r1)]:
        (target / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    manifest = dict(schemaVersion='t12-pilot-eval-v1', status='LOCKED_OFFLINE_DATA_ONLY', hotelId=4,
                    externalProcessingApproval='PENDING', semanticExecution='NOT_RUN',
                    generationSuccessMustNotIncludeFallback=True,
                    fileSha256={p.name: digest(p.read_bytes()) for p in target.iterdir()},
                    protectedEvaluationSha256=protected,
                    sourceApproval='Five public source contents approved previously; Gemini ingestion is a separate reviewed operation')
    (target / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('Created locked pack: 5 documents, 20 RAG cases, 12 R1 scenario specifications; no application/model calls')

if __name__ == '__main__':
    main()
