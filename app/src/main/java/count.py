import os

count = 0
for i in os.walk("."):
    for j in i[2]:
        if j.split('.')[-1] == 'java':
            with open(i[0] + '\\' + j, 'r', encoding='utf8') as f:
                length = len(f.readlines())
                print(i[0] + '\\' + j, length, count + length)
                count += length
