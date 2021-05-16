package com.dojomanager.security.services;

import java.util.Collection;
import java.util.Collections;

import com.dojomanager.data.entities.dojo.DojoOwner;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class OwnerPrincipal implements UserDetails{

    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public OwnerPrincipal(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = Collections.emptyList();
    }
    
    public OwnerPrincipal(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public static OwnerPrincipal build(DojoOwner owner) {
    return new OwnerPrincipal(owner.getId(), owner.getEmail(), owner.getPassword());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isEnabled() {
        // TODO Auto-generated method stub
        return true;
    }
    
    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        OwnerPrincipal owner = (OwnerPrincipal) o;
        return Objects.equal(id, owner.id);
    }
}
