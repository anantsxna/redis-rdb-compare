# redis-rdb-compare

[![CI build and test](https://github.com/anantsxna/redis-rdb-compare/actions/workflows/maven.yml/badge.svg)](https://github.com/anantsxna/redis-rdb-compare/actions/workflows/maven.yml)
<img src="https://img.shields.io/badge/vulnerabilities-0-green">
<a href="https://codecov.io/gh/anantsxna/redis-rdb-compare">
  <img src="https://codecov.io/gh/anantsxna/redis-rdb-compare/branch/main/graph/badge.svg?token=8L10DMFFRI"/>
</a>
<img src="https://img.shields.io/badge/maven-v3.8.1-blue">
<img src="https://img.shields.io/badge/openJDK-18-blue"> 
<img src="https://img.shields.io/badge/PyPy3-v7.3.9-blue">


    

A tool for comparing 2 Redis snapshots (.rdb files). The tool parses the .rdb files to extract the keys and answers queries via a slack bot.

## Installation Guide
- Since the repository depends on submodules, use the `--recurse-submodules` while cloning.
  ```
  git clone --recurse-submodules git@github.com:anantsxna/redis-rdb-compare.git
  ```

- Install [PyPy3](https://www.pypy.org/) (v.7.3.9 or above)

  After that, run:
  ```
  pip_pypy3 install python-lzf
  ```
  
  Move inside the `redis-rdb-compare/redis-rdb-tools/` directory of the repository and run:
  ```
  pypy3 setup.py install
  ```
  This will install the necessary packages to run the parsing tool.
  
- Install IDEA Intellij (v.2022.1 or above) or some other IDE and you can open the `redis-rdb-compare/redis-rdb-compare/` directory, which is a Maven project.


## Running the code
- Main Class contains the driver code. 
- Build and Run the Main class.
- The program automatically looks for 2 Redis Database file dumps (named `dump-A.rdb` and `dump-B.rdb`) inside the main directory. But it also allows you to input the absolute path of the files.
- The program parses the files parallelly and stores the keys in `keys-A.txt` and `keys-B.txt` files in the main directory.
  [Download sample .rdb files here.](https://drive.google.com/drive/folders/1VvFPBn-pJBUBAgcz9VFpQ-sBKCACo5d8?usp=sharing) Put the files in the `redis-rdb-compare/` main directory or specify absolute path at the start of the program.
- The program creates 2 Tries, for either file of keys (tokenized on `/` character and skipping the last token) and opens a menu in command line to answer queries.
- 2 types of queries are supported:
  1. In each database, how many keys begin with a certain prefix? 
  2. For a fixed prefix, what are the 'n' most common continuations up to the next level of the each trie?
  **(Both queries accept empty string as a prefix that applies to all keys of the database.)**
- Check the logs in `redis-rdb-compare/logs/` directory after the execution for detailed information about the execution.

## Java Classes
- **org.example:** Driver Code for running the app locally and on the Slack Bot, Utility methods to handle command/payload processing.
- **org.processing:** Parser class for the .rdb files.
- **org.querying:** Query interfcae and classes. 
- **org.trie:** Trie and TrieNode implementation.

