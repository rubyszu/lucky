# -*- coding: utf-8 -*-
import os, sys

current_file_path = os.path.dirname(__file__)
sys.path.append(os.path.realpath(os.path.join(current_file_path, '../../../')))
from config import GlobalVariable, branch
from jsonschema import validate
import time
import requests
import json
import unittest
reload(sys)
sys.setdefaultencoding('utf-8')
args = branch.get_args()
branch = args[0]

def request(variable):
	url = variable["url"]
	team_uuid = variable["team_uuid"]
	owner_uuid = variable["owner_uuid"]
	owner_token = variable["owner_token"]

	api_url = "%s/team/%s/pipelines" %(url,team_uuid)
	headers = {
		"Ones-Auth-Token": "%s" %(owner_token),
		"Ones-User-Id": "%s" %(owner_uuid)
	}
	print(headers)
	r = requests.get(api_url,headers = headers)
	return r

class TestGetPipelines(unittest.TestCase):
	def setUp(self):
		# self.setting = GlobalVariable("./config/setting.json").json
		# self.global_variable = GlobalVariable("./config/variable_%s.json" %(self.setting["branch"]))
		self.global_variable = GlobalVariable("./config/variable_%s.json" %(branch))
		self.variable = self.global_variable.json
		self.request = request(self.variable)
		self.status_code = self.request.status_code
		self.response_json = self.request.json()

	def test_result_200(self):
		#status code
		self.assertEqual(200,self.status_code)
		if(self.status_code != 200):
			return self.status_code
		#response body
		api_schema = GlobalVariable("./api_schema/pipeline/pipeline/get_pipelines_200.json").json
		validate(self.response_json, api_schema)

		#store data
		pipelines = self.response_json.get("pipelines")
		uuids = []
		for i in range(len(pipelines)):
			uuids.append(pipelines[i].get("uuid"))

		if(self.variable.__contains__("pipeline_uuids")):
			pipeline_uuids = self.variable["owner_uuid"]
			pipeline_uuids = uuids
		else:
			pipeline_uuids = uuids
		self.global_variable.store("pipeline_uuids",uuids)

		
		# write to json file
		self.global_variable.write()
		with open('response1.json','w') as f:
			f.write(self.request.text)

	def teardown(self):
		pass
		

def main():
	unittest.main(argv=args[1])
	
if __name__ == '__main__':
	main()