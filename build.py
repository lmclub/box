#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import argparse
import re



def edit_gradle_file(version):
    if "v" in version:
        version = version.replace("v","")
    current_dir = os.getcwd()  # 获取当前目录
    parent_dir = os.path.abspath(os.path.join(current_dir, os.pardir,"tv"))  # 获取上级目录
    with open(os.path.join(parent_dir,"app","build.gradle"),"rb") as f:
        content = str(f.read(),encoding='utf-8')
    matches = re.findall('versionName "([^"]*)"', content)
    content = content.replace(matches[0],version)
    content = content.split("'proguard-rules.pro'")[0] + "'proguard-rules.pro'" + "\n\t\t\tsigningConfig signingConfigs.debug" +  content.split("'proguard-rules.pro'")[1].replace("\n\t\t\tsigningConfig signingConfigs.debug","")
    with open(os.path.join(parent_dir,"app","build.gradle"),"wb") as f:
        f.write(content.encode("utf-8"))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', type=str, default="v2.3.4")  ## 添加
    args = parser.parse_args()
    edit_gradle_file(args.version)
