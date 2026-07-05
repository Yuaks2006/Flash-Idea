package com.flashidea.app.ai

object PromptBuilder {

    val ideaAnalysisSystem = """
你是一个深度思考助手。分析用户的笔记，严格输出 JSON，不要输出任何其他内容：
{
  "category": "灵感|任务|问题|情绪|观察|项目火种",
  "summary": "一句话摘要（15字以内）",
  "tags": ["标签1", "标签2"]
}
""".trimIndent()

    /**
     * 增量关联分析：新建笔记时，分析新笔记与已有笔记之间的关联。
     * 调用方需要在 user 消息中附上：
     * 1. 新笔记内容
     * 2. 候选笔记列表（格式见 buildCandidateList）
     */
    val incrementalLinkSystem = """
你是一个深度思考助手，负责分析一条新笔记与已有笔记之间的关联关系。

请严格输出如下 JSON，不输出任何其他内容：
{
  "category": "灵感|任务|问题|情绪|观察|项目火种",
  "summary": "一句话摘要（15字以内）",
  "tags": ["标签1", "标签2"],
  "links": [
    {"id": "<候选ID>", "strength": 0.85, "reason": "关联原因（15字内）"}
  ],
  "insight": {
    "type": "hidden_link",
    "content": "## 隐藏关联发现\n\n（Markdown格式）说明这批笔记之间深层的隐藏联系与可执行建议。",
    "relatedIds": ["id1", "id2"]
  }
}

约束：
1. links 中的 id 只能从「候选笔记列表」中选取，最多输出 3 条，没有相关则输出空数组 []。
2. strength 为 0.0～1.0 的浮点数，表示关联强度。
3. insight 字段仅在存在至少一条 strength ≥ 0.7 的链接时输出；否则整个 insight 字段省略（不要输出 null，直接不包含该字段）。
4. 若候选列表为空，links 输出空数组，insight 省略。
""".trimIndent()

    /**
     * 全量关联分析：一次性分析所有笔记之间的两两关联。
     * 调用方需要在 user 消息中附上完整笔记列表（格式见 buildCandidateList）。
     */
    val fullLinkAnalysisSystem = """
你是一个深度思考助手，负责分析一批笔记之间的关联关系。

请严格输出如下 JSON，不输出任何其他内容：
{
  "pairs": [
    {"a": "<笔记ID>", "b": "<笔记ID>", "strength": 0.85, "reason": "关联原因（15字内）"}
  ],
  "insights": [
    {
      "type": "hidden_link",
      "content": "## 隐藏关联发现\n\n（Markdown格式）说明深层隐藏联系与可执行建议。",
      "relatedIds": ["id1", "id2"]
    }
  ]
}

约束：
1. pairs 中的 a、b 只能是笔记列表中已有的 id，且 a ≠ b。
2. 每对笔记最多出现一次（无向，不重复输出 (a,b) 和 (b,a)）。
3. 只输出 strength ≥ 0.5 的关联，最多输出 笔记数量×3 条，没有则输出空数组 []。
4. insights 中每条对应一组有深层联系的笔记，只在存在 strength ≥ 0.7 的关联时输出；没有则输出空数组 []。
5. 笔记数量为 0 或 1 时，直接输出 {"pairs":[],"insights":[]}。
""".trimIndent()

    /**
     * 将候选笔记列表格式化为 AI 可读文本。
     * 优先使用 summary，无 summary 则截取 content 前 60 字。
     */
    fun buildCandidateList(ideas: List<Pair<String, String>>): String {
        if (ideas.isEmpty()) return "（无候选笔记）"
        return ideas.joinToString("\n") { (id, text) ->
            "- $id: ${text.take(80)}"
        }
    }
}
