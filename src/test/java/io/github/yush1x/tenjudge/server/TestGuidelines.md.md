# 测试规范

## 单元测试
- 使用mockito框架进行单元测试
- 测试方法命名：`被测试方法名_测试条件_预期结果`
- Mockito 使用宽松模式，不强制要求每个mock对象都被调用
- 业务逻辑中所有不方便单元测试的类都会被封装，如persistence包中的类通常封装与mybatis-plus的交互，StpService封装了Sa-Token的交互，请在mock时mock这些封装类而不是直接mock底层库的类