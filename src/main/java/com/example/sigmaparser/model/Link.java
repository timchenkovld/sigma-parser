package com.example.sigmaparser.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    private LinkType linkType;
    private String url;
    private String name;
    private Link parent;
    private List<Link> childLinks;

    @Override
    public String toString() {
        return "Link{" +
                "linkType=" + linkType +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", parent=" + (parent != null ? parent.getName() : "") +
                '}';
    }
}
