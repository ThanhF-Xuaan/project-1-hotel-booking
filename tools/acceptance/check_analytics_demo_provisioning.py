"""Destructive checks restricted to the explicit disposable m1_analytics_test database."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import analytics_demo_db as demo

def main():
    p=argparse.ArgumentParser();p.add_argument('--container',required=True);p.add_argument('--evidence-directory',required=True)
    args=p.parse_args();out=Path(args.evidence_directory)
    if out.exists(): raise ValueError('EVIDENCE_EXISTS')
    out.mkdir(parents=True)
    db=demo.LocalDatabase(args.container,'m1_analytics_test');sources=demo.load_sources()
    initial=demo.inspect(db,sources);assert len(initial)==2
    roles=db.query("SELECT count(*) FROM pg_roles WHERE rolname LIKE 'm1_demo_reader_%'")
    def cli(action,label,dataset='all'):
        r=subprocess.run([sys.executable,str(Path(demo.__file__)), '--action',action,'--container',args.container,'--database','m1_analytics_test','--dataset',dataset,'--evidence-directory',str(out/label)],capture_output=True,text=True)
        return r.returncode
    assert cli('seed','noop')==0
    assert roles==db.query("SELECT count(*) FROM pg_roles WHERE rolname LIKE 'm1_demo_reader_%'")
    try:
        db.query("UPDATE m1_demo.group_quotas SET room_count=room_count+1 WHERE dataset_key='group-v1'")
        assert cli('seed','drift-seed')==1
        assert cli('rollback','drift-rollback')==1
    finally: db.query("UPDATE m1_demo.group_quotas SET room_count=room_count-1 WHERE dataset_key='group-v1'")
    assert cli('rollback','selective','occupancy-v1')==0
    after=demo.inspect(db,sources);assert len(after)==1 and after[0]['payload']==next(r for r in initial if r['dataset_key']=='group-v1')['payload']
    # Failing statement after inserts must roll back all occupancy rows and its manifest.
    try:
        db.query('BEGIN;'+demo.seed_sql({'occupancy-v1':sources['occupancy-v1']})+'SELECT 1/0; COMMIT;')
        raise AssertionError('Expected transaction failure')
    except ValueError: pass
    assert db.query('SELECT count(*) FROM m1_demo.occupancy_samples')=='0'
    assert len(demo.inspect(db,sources))==1
    # Restore only this test dataset, preserve the original test reader and all other datasets.
    db.query('BEGIN;'+demo.seed_sql({'occupancy-v1':sources['occupancy-v1']})+'COMMIT;')
    assert len(demo.inspect(db,sources))==2
    (out/'receipt.json').write_text(json.dumps({'status':'PASS','checks':['no-op-no-new-role','drift-seed-rejected','drift-rollback-rejected','selective-rollback-preserves-group','mid-transaction-failure-atomic','test-dataset-restored']},indent=2),encoding='utf-8')
    print('PASS: 6 provisioning checks; disposable dataset restored')

if __name__=='__main__': main()
