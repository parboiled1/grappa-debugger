package com.github.fge.grappa.debugger.javafx.model;

import javax.annotation.concurrent.Immutable;

@Immutable
final public class MatchFragments
{

    final private String beforeMatch;
    final private String match;
    final private String afterMatch;

    public MatchFragments(String beforeMatch, String match, String afterMatch)
    {
        this.beforeMatch = beforeMatch;
        this.match = match;
        this.afterMatch = afterMatch;
    }

    public String getBeforeMatch() {
        return beforeMatch;
    }

    public String getMatch() {
        return match;
    }

    public String getAfterMatch() {
        return afterMatch;
    }

}
