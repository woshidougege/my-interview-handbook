'use client';

export default function RAGFlow() {
  return (
    <div className="my-8 bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl p-6 border-2 border-purple-300">
      <h4 className="text-2xl font-bold text-center mb-6 text-purple-600">
        RAG 完整流程
      </h4>
      
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* 步骤1 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-orange-500">
          <div className="text-2xl mb-2">1️⃣</div>
          <h5 className="font-bold text-orange-600 mb-2">文档处理</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• PDF/Word解析</li>
            <li>• 智能切片</li>
            <li>• 500-1000字/段</li>
          </ul>
        </div>

        {/* 步骤2 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-blue-500">
          <div className="text-2xl mb-2">2️⃣</div>
          <h5 className="font-bold text-blue-600 mb-2">向量化</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• BGE-M3模型</li>
            <li>• 1024维向量</li>
            <li>• 语义编码</li>
          </ul>
        </div>

        {/* 步骤3 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-green-500">
          <div className="text-2xl mb-2">3️⃣</div>
          <h5 className="font-bold text-green-600 mb-2">向量存储</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• Milvus数据库</li>
            <li>• HNSW索引</li>
            <li>• 百万级文档</li>
          </ul>
        </div>

        {/* 步骤4 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-red-500">
          <div className="text-2xl mb-2">4️⃣</div>
          <h5 className="font-bold text-red-600 mb-2">用户查询</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• "Flink怎么部署？"</li>
            <li>• 查询向量化</li>
            <li>• [0.2, 0.8, ...]</li>
          </ul>
        </div>

        {/* 步骤5 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-purple-500">
          <div className="text-2xl mb-2">5️⃣</div>
          <h5 className="font-bold text-purple-600 mb-2">混合检索</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• 向量检索Top20</li>
            <li>• BM25关键词</li>
            <li>• 混合召回</li>
            <li>• 延迟&lt;50ms</li>
          </ul>
        </div>

        {/* 步骤6 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-cyan-500">
          <div className="text-2xl mb-2">6️⃣</div>
          <h5 className="font-bold text-cyan-600 mb-2">重排序</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• gte-Qwen2-7B</li>
            <li>• 精准打分</li>
            <li>• 选Top3-5</li>
            <li>• 最相关文档</li>
          </ul>
        </div>

        {/* 步骤7 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-green-500">
          <div className="text-2xl mb-2">7️⃣</div>
          <h5 className="font-bold text-green-600 mb-2">生成答案</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• Qwen2.5-14B</li>
            <li>• 基于文档回答</li>
            <li>• 标注来源</li>
            <li>• 可追溯</li>
          </ul>
        </div>

        {/* 步骤8 */}
        <div className="bg-white rounded-lg p-4 shadow-md border-l-4 border-orange-500">
          <div className="text-2xl mb-2">8️⃣</div>
          <h5 className="font-bold text-orange-600 mb-2">返回用户</h5>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• 答案 + 来源文档</li>
            <li>• 支持追溯验证</li>
            <li>• 提升可信度</li>
          </ul>
        </div>
      </div>

      {/* 性能指标 */}
      <div className="mt-6 bg-white rounded-lg p-4 shadow-md">
        <h5 className="font-bold text-center mb-3 text-gray-800">性能指标</h5>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
          <div>
            <div className="text-2xl font-bold text-purple-600">~50ms</div>
            <div className="text-sm text-gray-600">检索延迟</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-green-600">95%+</div>
            <div className="text-sm text-gray-600">召回率</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-blue-600">90%+</div>
            <div className="text-sm text-gray-600">准确率</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-orange-600">百万级</div>
            <div className="text-sm text-gray-600">文档支持</div>
          </div>
        </div>
      </div>

      <div className="mt-4 text-center text-sm text-gray-600">
        💡 <strong>核心优势：</strong>基于真实文档回答，可追溯验证，减少幻觉
      </div>
    </div>
  );
}

