package com.up.smfmc;

/**
 *
 * @author Ricky
 */
public class Diff {
    public String file;
    public String line;
    public String content;
    public Method method;
    
    public static enum Method {
        after, before, replace, end;
    }

    public Diff(String file, String line, String content, Method method) {
        this.file = file.replace("./Sources", "$sourcedir").replace("./Themes", "$themedir");
        this.line = line;
        this.content = content;
        this.method = method;
    }
    
    @Override
    public String toString() {
        return  "	<file name=\"" + file + "\">\n" +
                "		<operation>\n" +
                "			<search position=\"" + method.name() + "\"" + (method == Method.end ? " />" : "><![CDATA[" + line + "]]></search>") + "\n" +
                "			<add><![CDATA[" + content + "]]></add>\n" +
                "		</operation>\n" +
                "	</file>";
    }
}
