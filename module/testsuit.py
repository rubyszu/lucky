# -*- coding: utf-8 -*-

import unittest
import ones

class TestMathFunc(unittest.TestCase):
    """Test mathfuc.py"""

    def test_add(self):
        """Test method add(a, b)"""
        self.assertEqual(3, 1 + 2)
        self.assertNotEqual(3, 2 + 2)

    def test_minus(self):
        """Test method minus(a, b)"""
        self.assertEqual(1, 3 - 2)

    def test_multi(self):
        """Test method multi(a, b)"""
        self.assertEqual(6, 2 * 3)

    # @unittest.skip("demonstrating skipping")
    # def test_skipped(self):
    #     self.fail("shouldn't happen")

    def test_divide(self):
        """Test method divide(a, b)"""
        self.assertEqual(2, 6 / 3)
        self.assertEqual(2, 5 / 2)

if __name__ == '__main__':
    suite = unittest.TestSuite()

    tests = [TestMathFunc("test_add"),TestMathFunc("test_minus"),TestMathFunc("test_multi"),TestMathFunc("test_divide")]
    suite.addTests(tests)
    
    # url = "https://api.ones.team/project/F5001/team/VnfMZEQS/pipeline/EEbASw86/callback?token=RJtj95uknZhU8FqPzHMY1x"
    runner = ones.OnesTestRunner()
    runner.run(suite)