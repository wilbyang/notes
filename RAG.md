```python
import pandas as pd

data = pd.read_csv("/Users/xupeng/Downloads/sentence.csv", sep='\\t')
data.head()
sentences = data['sentence'].tolist()
print("sentences length: {}".format(len(sentences)))
from sentence_transformers import SentenceTransformer

# 初始化文本向量化模型
model = SentenceTransformer('bert-base-nli-mean-tokens')

# 将文本向量化
sentence_embeddings = model.encode(sentences)
# 获取向量化后向量的维度
d = sentence_embeddings.shape[1]

import faiss

# 创建faiss索引
index = faiss.IndexFlatL2(d)
# flat索引无需训练，所以这里输出为true
print("is_trained: {}".format(index.is_trained))
# 将数据添加到faiss索引
index.add(sentence_embeddings)

k = 5
xq = model.encode(["Someone sprints with a football"])

import time
start_time = time.time()
# 搜索TOP 5最相似向量
D, I = index.search(xq, k)  # search
end_time = time.time()
execution_time = (end_time - start_time)*1000
print(f"执行时间: {execution_time} 毫秒")
print(I)
# 根据搜索结果位置找出原数据
print(data['sentence'].iloc[I.flatten().tolist()])
```


![index types](/Users/boya/sites/geek_images/847/4d50087d8478c9d326f751387223aff0.png)