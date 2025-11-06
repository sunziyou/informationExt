package org.example.parser;


public class DeepseekParserTool implements Parser {


    @Override
    public String parseJson(String toolContent) {
        toolContent=toolContent.trim();
        if(toolContent.contains("```json")){
            toolContent=toolContent.substring(toolContent.lastIndexOf("```json")+7);
        }
        if(toolContent.contains("{")&&toolContent.contains("}")){
            toolContent=toolContent.substring(toolContent.indexOf("{"),toolContent.lastIndexOf("}")+1);
            return toolContent;
        }
        return null;
    }

}
