#!/bin/bash
PATH=${PATH}:/usr/local/bin

#pip install -U pytest
# py.test --junitxml results.xml tests.py

# cd module/test && python testsuit.py
# python run/run_all_test.py
for i in {1..1000}
do
	python module/task/add_one_task.py --branch=$1
done

# cd module/login && py.test --junitxml results.xml test_login_200.py