"""Keep all historical audits intact after verifying the additive U22 route."""
import pathlib
import subprocess

APP = 'frontend/src/App.tsx'
SCRIPT = pathlib.Path(__file__).with_suffix('.cjs')


def reconcile(raw):
    result = subprocess.run(['node', str(SCRIPT)], input=raw, capture_output=True)
    if result.returncode:
        raise ValueError('App differs beyond exact CHAIN_ADMIN U22 lazy route')
    return result.stdout
