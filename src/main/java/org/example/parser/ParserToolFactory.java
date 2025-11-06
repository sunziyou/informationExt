package org.example.parser;

public class ParserToolFactory {
    public static Parser createParserTool(String moduleName)
    {
        if(moduleName.toLowerCase().contains("deepseek"))
        {
            return new DeepseekParserTool();
        }
        return new DeepseekParserTool();
    }

}
