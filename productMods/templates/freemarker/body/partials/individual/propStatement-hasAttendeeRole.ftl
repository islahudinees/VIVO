<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Custom object property statement view for http://vivoweb.org/ontology/core#hasAttendeeRole.
    
     This template must be self-contained and not rely on other variables set for the individual page, because it
     is also used to generate the property statement during a deletion.  
 -->

<#import "lib-sequence.ftl" as s>
<#import "lib-datetime.ftl" as dt>

<@showRole statement />

<#-- Use a macro to keep variable assignments local; otherwise the values carry over to the
     next statement -->
<#macro showRole statement>
    <#local linkedIndividual>
        <#if statement.event1??>
            <a href="${profileUrl(statement.event1)}">${statement.event1Label!statement.event1Name}</a>
        <#else>
            <#-- This shouldn't happen, but we must provide for it -->
            <a href="${profileUrl(statement.role)}">missing event</a>
        </#if>
    </#local>
    
    <#local dateTime>
        <@dt.yearSpan statement.dateTime! /> 
    </#local>
    
    <#local superEvent>
        <#if statement.event2?has_content && statement.event2Label?has_content>
            <a href="${profileUrl(statement.event2)}">${statement.event2Label}</a>
        <#elseif statement.series?has_content && statement.seriesLabel?has_content>
            <a href="${profileUrl(statement.series)}">${statement.seriesLabel}</a>
        </#if>
    </#local>

    <@s.join [ linkedIndividual, statement.roleLabel!, superEvent! ] /> ${dateTime!}

</#macro>