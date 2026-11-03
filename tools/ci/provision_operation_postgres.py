"""Provision one CI-owned synthetic operation DB. Never connect to an external database."""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import time

ROOT=Path(__file__).resolve().parents[2]
NAME='m1-operation-ci'
OWNERSHIP='vn.edu.utc.hotel-booking.operation-it'

def docker(*args, input=None, check=True):
    result=subprocess.run(['docker',*args],input=input,capture_output=True,timeout=180)
    if check and result.returncode:
        raise RuntimeError('Owned PostgreSQL CI command failed; no credentials or row contents emitted')
    return result

def require_ci():
    if os.environ.get('CI')!='true' or not os.environ.get('GITHUB_ACTIONS'):
        raise RuntimeError('Operation DB provisioner only runs inside isolated GitHub Actions jobs')

def cleanup():
    require_ci()
    result=docker('container','inspect',NAME,check=False)
    if result.returncode: return
    details=json.loads(result.stdout)[0]
    if details['Config'].get('Labels',{}).get(OWNERSHIP)!='true':
        raise RuntimeError('Refusing to remove a container without the owned operation-IT label')
    docker('rm','-f',NAME)

def start():
    require_ci()
    if docker('container','inspect',NAME,check=False).returncode==0:
        raise RuntimeError('Operation test container already exists; never overwrite it')
    with socket.socket() as connection:
        if connection.connect_ex(('127.0.0.1',15432))==0:
            raise RuntimeError('Operation test port already occupied')
    password=secrets.token_hex(24)
    print('::add-mask::'+password,flush=True)
    docker('run','-d','--name',NAME,'--label',OWNERSHIP+'=true',
           '-e','POSTGRES_DB=hotel_acceptance_db','-e','POSTGRES_USER=postgres',
           '-e','POSTGRES_PASSWORD='+password,'-p','127.0.0.1:15432:5432','postgres:16-alpine')
    for attempt in range(40):
        result=docker('exec',NAME,'psql','-X','-qAt','-h','127.0.0.1','-U','postgres',
                      '-d','hotel_acceptance_db','-c','SELECT 1',check=False)
        if result.returncode==0 and result.stdout.strip()==b'1': break
        time.sleep(1)
    else: raise RuntimeError('Owned PostgreSQL was not ready')
    docker('run','--rm','--network','container:'+NAME,
           '-v',str(ROOT/'backend/src/main/resources/db')+':/liquibase/changelog/db:ro',
           '-e','LIQUIBASE_SEARCH_PATH=/liquibase/changelog',
           '-e','LIQUIBASE_COMMAND_URL=jdbc:postgresql://127.0.0.1:5432/hotel_acceptance_db',
           '-e','LIQUIBASE_COMMAND_USERNAME=postgres','-e','LIQUIBASE_COMMAND_PASSWORD='+password,
           '-e','LIQUIBASE_COMMAND_CONTEXTS=master-data,m1-active-stay-dedup',
           'liquibase/liquibase:4.31-alpine','--search-path=/liquibase/changelog',
           '--changelog-file=db/db-changelog-root.xml','update')
    docker('exec','-i',NAME,'psql','-X','-v','ON_ERROR_STOP=1',
           '-v','u14_customer_b_subject=20000000-0000-0000-0000-000000000007',
           '-U','postgres','-d','hotel_acceptance_db',
           input=(ROOT/'tools/acceptance/fixtures/acceptance-fixtures.sql').read_bytes())
    binding=docker('port',NAME,'5432/tcp').stdout.decode().strip()
    connections=docker('exec',NAME,'psql','-X','-qAt','-U','postgres','-d','hotel_acceptance_db','-c',
                       'SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid()').stdout.strip()
    if binding!='127.0.0.1:15432' or connections!=b'0':
        raise RuntimeError('Cannot establish exclusive synthetic operation target')
    values={'M1_OPERATION_IT':'true','T05_EXPORT_IT':'true','T05_EXPORT_IT_EXCLUSIVE_VERIFIED':'true',
            'T05_EXPORT_IT_DB_HOST':'127.0.0.1','T05_EXPORT_IT_DB_PORT':'15432',
            'T05_EXPORT_IT_DB_NAME':'hotel_acceptance_db','T05_EXPORT_IT_DB_USER':'postgres',
            'T05_EXPORT_IT_DB_PASSWORD':password}
    with Path(os.environ['GITHUB_ENV']).open('a',encoding='utf-8') as output:
        for key,value in values.items(): output.write(key+'='+value+'\n')
    print('Owned synthetic operation PostgreSQL ready; explicit maintenance migration applied')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action',choices=('start','cleanup'))
    args=parser.parse_args()
    {'start':start,'cleanup':cleanup}[args.action]()
