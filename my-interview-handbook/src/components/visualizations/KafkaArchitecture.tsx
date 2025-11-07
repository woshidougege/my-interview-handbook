'use client';

export default function KafkaArchitecture() {
  return (
    <div className="my-8 bg-gradient-to-br from-yellow-50 to-orange-50 rounded-xl p-6 border-2 border-orange-300">
      <h4 className="text-2xl font-bold text-center mb-6 text-orange-600">
        Kafka 工作原理
      </h4>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Producer */}
        <div className="bg-white rounded-lg p-5 shadow-lg border-t-4 border-blue-500">
          <div className="text-center mb-4">
            <div className="text-4xl mb-2">📤</div>
            <h5 className="text-xl font-bold text-blue-600">Producer 生产者</h5>
          </div>
          <ul className="text-sm text-gray-700 space-y-2">
            <li>✓ 发送消息到Topic</li>
            <li>✓ 指定分区策略</li>
            <li>✓ acks确认机制</li>
            <li>✓ 批量发送优化</li>
          </ul>
        </div>

        {/* Broker */}
        <div className="bg-white rounded-lg p-5 shadow-lg border-t-4 border-green-500">
          <div className="text-center mb-4">
            <div className="text-4xl mb-2">🗄️</div>
            <h5 className="text-xl font-bold text-green-600">Broker 集群</h5>
          </div>
          <div className="mb-3">
            <h6 className="font-semibold text-gray-800 mb-2">Topic & Partition</h6>
            <div className="bg-green-50 rounded p-3 text-xs">
              <div className="mb-1">Topic: my-topic</div>
              <div className="grid grid-cols-4 gap-1">
                <div className="bg-green-200 p-1 text-center">P0</div>
                <div className="bg-green-200 p-1 text-center">P1</div>
                <div className="bg-green-200 p-1 text-center">P2</div>
                <div className="bg-green-200 p-1 text-center">P3</div>
              </div>
            </div>
          </div>
          <ul className="text-sm text-gray-700 space-y-1">
            <li>• 数据持久化</li>
            <li>• 副本机制（3副本）</li>
            <li>• Leader/Follower</li>
          </ul>
        </div>

        {/* Consumer */}
        <div className="bg-white rounded-lg p-5 shadow-lg border-t-4 border-purple-500">
          <div className="text-center mb-4">
            <div className="text-4xl mb-2">📥</div>
            <h5 className="text-xl font-bold text-purple-600">Consumer 消费者</h5>
          </div>
          <ul className="text-sm text-gray-700 space-y-2">
            <li>✓ 订阅Topic</li>
            <li>✓ Consumer Group</li>
            <li>✓ Offset管理</li>
            <li>✓ 负载均衡</li>
          </ul>
          <div className="mt-3 bg-purple-50 rounded p-2 text-xs">
            <div className="font-semibold mb-1">消费者组</div>
            <div>Group-A: C1, C2, C3, C4</div>
          </div>
        </div>
      </div>

      {/* 分区机制说明 */}
      <div className="mt-6 bg-white rounded-lg p-5 shadow-lg">
        <h5 className="font-bold text-gray-800 mb-3 text-center">📦 Kafka分区机制</h5>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="text-center">
            <div className="text-3xl mb-2">⚡</div>
            <h6 className="font-semibold text-gray-800 mb-1">并行处理</h6>
            <p className="text-sm text-gray-600">多分区同时读写<br/>提升吞吐量</p>
          </div>
          <div className="text-center">
            <div className="text-3xl mb-2">🎯</div>
            <h6 className="font-semibold text-gray-800 mb-1">负载均衡</h6>
            <p className="text-sm text-gray-600">分区分配给不同消费者<br/>均衡负载</p>
          </div>
          <div className="text-center">
            <div className="text-3xl mb-2">✓</div>
            <h6 className="font-semibold text-gray-800 mb-1">顺序保证</h6>
            <p className="text-sm text-gray-600">同一分区内有序<br/>相同Key→同一分区</p>
          </div>
        </div>
      </div>

      {/* 项目实践 */}
      <div className="mt-4 bg-orange-100 rounded-lg p-4">
        <h5 className="font-bold text-orange-800 mb-2">🎯 项目实践</h5>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
          <div>
            <span className="font-semibold text-orange-700">核心Topic：</span>
            <span className="text-gray-700">256分区</span>
          </div>
          <div>
            <span className="font-semibold text-orange-700">配合：</span>
            <span className="text-gray-700">Flink 128并行度</span>
          </div>
          <div>
            <span className="font-semibold text-orange-700">吞吐量：</span>
            <span className="text-gray-700">百万级QPS</span>
          </div>
        </div>
      </div>
    </div>
  );
}

