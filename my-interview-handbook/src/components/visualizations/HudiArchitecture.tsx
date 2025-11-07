'use client';

export default function HudiArchitecture() {
  return (
    <div className="my-8 bg-gradient-to-br from-green-50 to-cyan-50 rounded-xl p-6 border-2 border-green-300">
      <h4 className="text-2xl font-bold text-center mb-6 text-green-600">
        Hudi 数据湖架构
      </h4>
      
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* 数据源 */}
        <div className="space-y-3">
          <div className="bg-blue-100 rounded-lg p-4 shadow-md">
            <div className="text-center mb-2">
              <div className="text-3xl">⚡</div>
              <h5 className="font-bold text-blue-700">Flink</h5>
            </div>
            <ul className="text-xs text-gray-700">
              <li>• 实时计算</li>
              <li>• 数据清洗</li>
            </ul>
          </div>
          
          <div className="bg-yellow-100 rounded-lg p-4 shadow-md">
            <div className="text-center mb-2">
              <div className="text-3xl">📨</div>
              <h5 className="font-bold text-yellow-700">Kafka</h5>
            </div>
            <ul className="text-xs text-gray-700">
              <li>• 原始数据流</li>
              <li>• 消息队列</li>
            </ul>
          </div>
        </div>

        {/* Hudi核心 */}
        <div className="lg:col-span-2 bg-gradient-to-br from-green-100 to-green-200 rounded-lg p-5 shadow-lg">
          <div className="text-center mb-4">
            <div className="text-4xl mb-2">🌊</div>
            <h5 className="text-2xl font-bold text-green-700">Apache Hudi</h5>
          </div>
          
          <div className="grid grid-cols-2 gap-3 mb-3">
            <div className="bg-white rounded p-3">
              <h6 className="font-semibold text-green-700 mb-2 text-sm">Timeline</h6>
              <ul className="text-xs text-gray-600">
                <li>• 记录所有操作</li>
                <li>• MVCC版本控制</li>
              </ul>
            </div>
            
            <div className="bg-white rounded p-3">
              <h6 className="font-semibold text-green-700 mb-2 text-sm">Index</h6>
              <ul className="text-xs text-gray-600">
                <li>• 快速定位记录</li>
                <li>• BloomFilter索引</li>
              </ul>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="bg-blue-50 rounded p-3 border-2 border-blue-300">
              <h6 className="font-bold text-blue-700 text-center mb-2">COW表</h6>
              <ul className="text-xs text-gray-700">
                <li>✓ 查询快</li>
                <li>✓ 适合实时增量</li>
              </ul>
            </div>
            
            <div className="bg-yellow-50 rounded p-3 border-2 border-yellow-300">
              <h6 className="font-bold text-yellow-700 text-center mb-2">MOR表</h6>
              <ul className="text-xs text-gray-700">
                <li>✓ 写入快</li>
                <li>✓ 适合CDC同步</li>
              </ul>
            </div>
          </div>

          <div className="mt-3 bg-purple-50 rounded p-3">
            <h6 className="font-semibold text-purple-700 text-center mb-1 text-sm">元数据同步</h6>
            <p className="text-xs text-gray-600 text-center">自动同步到Hive Metastore</p>
          </div>
        </div>

        {/* 存储和查询 */}
        <div className="space-y-3">
          <div className="bg-cyan-100 rounded-lg p-4 shadow-md">
            <div className="text-center mb-2">
              <div className="text-3xl">💾</div>
              <h5 className="font-bold text-cyan-700">HDFS</h5>
            </div>
            <ul className="text-xs text-gray-700">
              <li>• Parquet文件</li>
              <li>• 分区存储</li>
              <li>• 副本保证</li>
              <li>• 高可用</li>
            </ul>
          </div>
          
          <div className="bg-orange-100 rounded-lg p-4 shadow-md">
            <div className="text-center mb-2">
              <div className="text-3xl">🔍</div>
              <h5 className="font-bold text-orange-700">查询引擎</h5>
            </div>
            <ul className="text-xs text-gray-700">
              <li>• Hive</li>
              <li>• Spark</li>
              <li>• Presto</li>
              <li>• Impala</li>
            </ul>
            <div className="mt-2 text-xs font-semibold text-orange-600 text-center">
              流批一体查询
            </div>
          </div>
        </div>
      </div>

      {/* 核心功能 */}
      <div className="mt-6 bg-white rounded-lg p-5 shadow-lg">
        <h5 className="font-bold text-gray-800 mb-3 text-center">🎯 核心功能</h5>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
          <div>
            <div className="text-2xl mb-1">🔄</div>
            <div className="font-semibold text-sm">流批一体</div>
            <div className="text-xs text-gray-600">实时写入+批量查询</div>
          </div>
          <div>
            <div className="text-2xl mb-1">✅</div>
            <div className="font-semibold text-sm">ACID事务</div>
            <div className="text-xs text-gray-600">数据一致性保证</div>
          </div>
          <div>
            <div className="text-2xl mb-1">📊</div>
            <div className="font-semibold text-sm">增量消费</div>
            <div className="text-xs text-gray-600">只读变更数据</div>
          </div>
          <div>
            <div className="text-2xl mb-1">⏰</div>
            <div className="font-semibold text-sm">时间旅行</div>
            <div className="text-xs text-gray-600">查询历史版本</div>
          </div>
        </div>
      </div>

      {/* 项目应用 */}
      <div className="mt-4 bg-green-100 rounded-lg p-4">
        <h5 className="font-bold text-green-800 mb-2">💼 项目中的应用</h5>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-2 text-sm">
          <div className="text-gray-700">• 实时数据入湖：Flink→Hudi→HDFS</div>
          <div className="text-gray-700">• CDC同步：捕获数据库变更</div>
          <div className="text-gray-700">• 数据更新：支持Upsert，不用全量替换</div>
        </div>
      </div>
    </div>
  );
}

