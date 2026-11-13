// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

public final class HandoffChecks {
    public static void main(String[]args){run();System.out.println("PWA scope, source hint and repository boundary checks passed");}
    public static void run(){
        check(HandoffPolicy.officialLyra("https://lyraedu.app/chat"), "Custom deployment missed");
        check(HandoffPolicy.officialLyra("https://lyra-dev.vercel.app/"), "Preview deployment missed");
        check(!HandoffPolicy.officialLyra("https://lyraedu.app.evil.test/chat"), "Lookalike deployment accepted");
        check(!HandoffPolicy.officialLyra("https://lyraedu.app@evil.test/chat"), "Credential URL accepted");
        check(!HandoffPolicy.officialLyra("http://lyraedu.app/chat"), "Unencrypted deployment accepted");
        check("https://lyraedu.app/chat?mode=learn".equals(HandoffPolicy.pwaDestination("https://lyra-dev.vercel.app/chat?mode=learn", "https://lyraedu.app/", "https://lyraedu.app/")), "Installed alias app was not selected");
        check(HandoffPolicy.pwaDestination("https://lyra-dev.vercel.app/chat", "https://lyraedu.app/other/", "https://lyraedu.app/other/") == null, "Alias escaped PWA scope");
        check(HandoffPolicy.pwaDestination("https://school.test/chat", "https://lyraedu.app/", "https://lyraedu.app/") == null, "Custom destination switched origins");
        check(HandoffPolicy.inScope("https://school.test/chat?m57_intent=new","https://school.test/", "https://school.test/"),"Root PWA missed");
        check(HandoffPolicy.inScope("https://school.test:443/chat/new","https://school.test/chat/","https://school.test/chat"),"HTTPS default port mismatch");
        check(HandoffPolicy.inScope("https://school.test","https://school.test/",null),"Empty path root missed");
        check(!HandoffPolicy.inScope("https://school.test/chatty","https://school.test/chat/","https://school.test/chat"),"Scope path boundary failed");
        check(!HandoffPolicy.inScope("https://other-school.test/chat","https://school.test/",null),"Wrong school PWA accepted");
        check(!HandoffPolicy.inScope("https://school.test/chat/%2e%2e/admin","https://school.test/chat/",null),"Ambiguous path accepted");
        check(!HandoffPolicy.inScope("https://school.test/chat","https://school.test/","https://evil.test/"),"Cross-origin scope accepted");
        check(HandoffPolicy.repositoryAddress("github.com/lyraedu/m57/issues"),"Repository guard missed");
        check(HandoffPolicy.repositoryAddress("github.com/akshay-abraham/m57/issues"),"Old repository redirect missed");
        check(HandoffPolicy.repositoryAddress("https://raw.githubusercontent.com/lyraedu/m57/main/README.md"),"Raw repo missed");
        check(!HandoffPolicy.repositoryAddress("https://github.com/lyraedu/m570"),"Other repository blocked");
        check(!HandoffPolicy.repositoryAddress("https://github.com.evil.test/lyraedu/m57"),"Lookalike blocked");
        check(!HandoffPolicy.repositoryAddress("https://github.com/another/m57"),"Other owner's repo blocked");
        check("chatgpt".equals(HandoffPolicy.source("chat.openai.com")),"ChatGPT hint missed");
        check("other".equals(HandoffPolicy.source("chatgpt.com.evil.test")),"Lookalike source matched");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
