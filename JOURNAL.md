## 使用记录
- 2026-06-01 搭建跑通开发环境
1. 安装clude code CLI （遇到代理问题，修改/配置终端代理解决网络问题）
2. 使用CC switch 进行模型的配置，目前使用的是deepseek-v4 进行项目开发；后续想配置免费的模式（Qwen3.7）对比效果，但是CC switch 不兼容；在尝试其他免费模型
3. 安装使用superpowers,学习了它的使用方法；
4. 创建github仓库，并提交init代码；

- 2026-06-02 项目开发
1. 使用 superpowers ，向CC 简单说明我要创建的项目，CC使用brainstorming 向我询问具体需求，之后生成Plan一步步执行（创建了git commit hook，每个task完成后git commit），
2. 初版本生成运行，没有达到预期效果，存在问题，首先页面结构和需求不一样，缺少关键字段，并且部分内容没有解析出来，并且解析文件没有使用LLM去做，而是使用正则方法做的，这也是错误的原因，
后续提示CC去用LLM解析简历内容；修改后简历内容可以完整解析，但是部分字段还是没有正确映射到页面；
3. 重新要求CC（❯ 限制上传的文件为.doc 等文本文件，不适用PDF，但导出的是PDF格式，文件内容，是基础，公司经历，和专业技能，仅仅优化专业技能达到高级开发水平。文件样式不要改变）
CC+SP重新问了关键需求点开始新的Plan去完成；
4. 完成后用 .doc文件测试报错（The supplied data appears to be in the OLE2 Format. You are calling the part of POI that deals with OOXML (Office Open XML) Documents. You need to call a different part of POI to process this data (eg HSSF instead of XSSF)，原因是.doc（旧版 Word OLE2 格式）和 .docx（新版 OOXML）需要不同的解析器。XWPFDocument 只处理 .docx，.doc 需要用 HWPFDocument。需要添加 poi-scratchpad 依赖（含 HWPFDocument 用于 .doc），然后修改 ParserService。



## 对Claude Code的认识变化


## "原来还能这样"的事


## "这玩意还不行"的事


## 优化计划


## 「换个角色」思考（200~500 字）：让一个完全没碰过 CC 的同事（产品候选人 → 假设技术同事；技术候选人 → 假设产品/运营同事）开始用 CC做本职工作，你会给他配什么 skill / agent / hook？为什么？