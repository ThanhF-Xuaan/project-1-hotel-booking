import json
import os
import subprocess
import unittest
from unittest.mock import patch
import provision_operation_postgres as provision

class OperationProvisioningTest(unittest.TestCase):
    def test_local_environment_is_refused_before_any_docker_call(self):
        with patch.dict(os.environ,{},clear=True),patch.object(provision,'docker') as docker:
            for action in (provision.start,provision.cleanup):
                with self.assertRaises(RuntimeError):action()
            docker.assert_not_called()
    def test_cleanup_refuses_unowned_existing_container(self):
        result=subprocess.CompletedProcess([],0,json.dumps([{'Config':{'Labels':{}}}]).encode(),b'')
        with patch.dict(os.environ,{'CI':'true','GITHUB_ACTIONS':'true'},clear=True),patch.object(provision,'docker',return_value=result) as docker:
            with self.assertRaises(RuntimeError):provision.cleanup()
            self.assertEqual(docker.call_count,1)
    def test_cleanup_only_removes_verified_owned_name(self):
        result=subprocess.CompletedProcess([],0,json.dumps([{'Config':{'Labels':{provision.OWNERSHIP:'true'}}}]).encode(),b'')
        with patch.dict(os.environ,{'CI':'true','GITHUB_ACTIONS':'true'},clear=True),patch.object(provision,'docker',return_value=result) as docker:
            provision.cleanup()
            self.assertEqual(docker.call_args.args,('rm','-f',provision.NAME))

if __name__=='__main__':unittest.main()
