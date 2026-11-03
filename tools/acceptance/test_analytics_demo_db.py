import unittest
import analytics_demo_db as provision

class AnalyticsDemoProvisioningTest(unittest.TestCase):
    def test_frozen_input_is_oracle_free_and_complete(self):
        data=provision.load_sources()
        self.assertEqual(840,sum(len(h['samples']) for h in data['occupancy-v1']['hotels']))
        self.assertEqual(7,len(data['group-v1']['cases']))
        self.assertNotIn('"expected"',provision.seed_sql(data))

    def test_scoped_keys_and_counts_in_seed(self):
        text=provision.seed_sql(provision.load_sources())
        self.assertIn('m1_demo.occupancy_samples',text)
        self.assertEqual(7,text.count('INSERT INTO m1_demo.group_scenarios'))
        self.assertNotIn('INSERT INTO public.',text)
        self.assertNotIn('ON CONFLICT',text)

    def test_sql_literals_preserve_quoted_values_without_execution(self):
        self.assertEqual("'a''b'",provision.sql("a'b"))
        self.assertEqual('NULL',provision.sql(None))
        self.assertEqual('false',provision.sql(False))

    def test_database_payload_order_is_stable(self):
        data=provision.load_sources()
        payload=provision.source_payload('group-v1',data['group-v1'])
        self.assertEqual(sorted(c['id'] for c in payload['cases']),[c['id'] for c in payload['cases']])

if __name__=='__main__': unittest.main()
