import base64, hashlib, os, re, sys, time, urllib.request, urllib.error

ROOT = '/root/develop/Party_Building_System'
EXPORT = os.path.join(ROOT, 'docs', 'export')
IMAGES = os.path.join(EXPORT, 'images')
os.makedirs(IMAGES, exist_ok=True)

DOCS = [
    ('docs/requirements/需求分析报告.md', '01-需求分析报告'),
    ('docs/feasibility/可行性研究报告.md', '02-可行性研究报告'),
    ('docs/design/总体设计说明书.md', '03-总体设计说明书'),
    ('docs/design/详细设计说明书.md', '04-详细设计说明书'),
]

FENCE = re.compile(r'```mermaid\s*\n(.*?)```', re.S)

def render(code, path):
    b64 = base64.urlsafe_b64encode(code.encode('utf-8')).decode('ascii')
    url = 'https://mermaid.ink/img/' + b64 + '?type=png&bgColor=FFFFFF'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
            if data[:8] == b'\x89PNG\r\n\x1a\n':
                with open(path, 'wb') as f:
                    f.write(data)
                return True
            print('  non-png response', len(data))
        except Exception as e:
            print('  attempt', attempt + 1, 'error:', e)
        time.sleep(2)
    return False

def main():
    failures = []
    for src, name in DOCS:
        src_path = os.path.join(ROOT, src)
        with open(src_path, encoding='utf-8') as f:
            text = f.read()
        blocks = FENCE.findall(text)
        print(f'{name}: {len(blocks)} diagrams')
        idx = 0
        def repl(m):
            nonlocal idx
            idx += 1
            code = m.group(1).strip()
            img_name = f'{name}-fig{idx:02d}.png'
            img_path = os.path.join(IMAGES, img_name)
            ok = render(code, img_path)
            if not ok:
                failures.append((name, idx, code.splitlines()[0]))
                return f'**图 {idx} 渲染失败，源码如下：**\n\n```\n{code}\n```'
            return f'![图 {idx}](images/{img_name}){{width=15cm}}'
        out = FENCE.sub(repl, text)
        out_path = os.path.join(EXPORT, name + '.md')
        with open(out_path, 'w', encoding='utf-8') as f:
            f.write(out)
        print('  wrote', out_path)
    if failures:
        print('FAILURES:')
        for x in failures:
            print(' ', x)
        sys.exit(1)

if __name__ == '__main__':
    main()