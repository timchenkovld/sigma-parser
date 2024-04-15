package com.example.sigmaparser.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(url, link.url) && Objects.equals(name, link.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, name);
    }
}
