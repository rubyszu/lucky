# -*- coding: utf-8 -*-
import os, sys

sys.path.append(os.path.realpath(os.path.join(os.path.dirname(__file__), '../../')))
try:
	import importlib
	importlib.reload(sys)
except Exception:
	reload(sys)
from config import GlobalVariable
import time
import requests
import json
import unittest

def request(variable):
	url = variable["url"]
	owner_email = variable["owner_email"]
	owner_password = variable["owner_password"]

	api_url = "%s/auth/login" %(url)
	headers = {
		"Content-Type": "application/json"
	}
	body = {
	  "email":"%s" %(owner_email),
	  "password":"%s" %(owner_password)
	}

	print("------------headers------------")
	print(headers)
	print("------------body--------------")
	print(body)

	r = requests.post(api_url, headers=headers, data=json.dumps(body))
	return r

class TestGroupSort(unittest.TestCase):
	def setUp(self):
		self.global_variable = GlobalVariable("../../config/variable.json")
		self.variable = self.global_variable.json
		self.request = request(self.variable)
		self.status_code = self.request.status_code
		self.response_json = self.request.json()

	def test_result_200(self):
		
		'''test login 200'''
		#status code
		print("----------status_code----------")
		print(self.status_code)
		self.assertEqual(200,self.status_code)
		if(self.status_code != 200):
			return self.status_code
		#response body
		print("------------response--------------")
		print(self.response_json)
		self.assertIn("user", self.response_json)
		self.assertIn("teams",self.response_json)

		user = self.response_json.get("user")
		print("-----------type(user)------------")
		print(type(user))
		print("-------------user-----------")
		print(user)
		token = user.get("token")
		print("-----------token-----------")
		print(token)

		if(self.variable.__contains__("owner_token")):
			owner_token = self.variable["owner_token"]
			owner_token = token
		else:
			owner_token = token
		self.global_variable.store("owner_token",token)
		# write to json file
		self.global_variable.write()

	def teardown(self):
		pass
		

def main():
	unittest.main(verbosity = 2)
	
if __name__ == '__main__':
	main()