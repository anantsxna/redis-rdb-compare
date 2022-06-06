
'''
This is a fast parser execution script for a single .rdb file to extract and store keys:
'''
import sys, argparse
from rdbtools import RdbParser
from rdbtools.callbacks import KeysOnlyCallback

import argparse, sys

parser=argparse.ArgumentParser()
parser.add_argument('--rdb', help='Dump file absolute path', required=True, type=str)
parser.add_argument('--keys', help='Store the keys at this absolute path', required=True, type=str)

if __name__ == '__main__':
    options, _ = parser.parse_known_args()
    with open(options.keys, "wb+") as keysFile:
        print('Parsing File: ' + options.rdb)
        callback = KeysOnlyCallback(keysFile)
        print('Callback Created')
        parser = RdbParser(callback)
        print('Parser Created')
        parser.parse(options.rdb)
        print('Parsing Complete')
        keysFile.close()
