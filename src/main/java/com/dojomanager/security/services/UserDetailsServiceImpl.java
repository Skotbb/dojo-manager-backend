package com.dojomanager.security.services;

import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.services.DojoOwnerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    DojoOwnerService ownerService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        DojoOwner owner = ownerService.getOwnerByEmail(username);

        return OwnerPrincipal.build(owner);
    }


}
