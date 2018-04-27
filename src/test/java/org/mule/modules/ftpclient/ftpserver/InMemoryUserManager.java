package org.mule.modules.ftpclient.ftpserver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

/**
 * Manage users for Apaches ftp server.
 */
public class InMemoryUserManager implements UserManager {
    private String adminName;
    private Map<String, User> userMap = new HashMap<>();

    public InMemoryUserManager(String adminName, String adminPassword, File home, int maxIdleTimeSeconds) {
        if (StringUtils.isEmpty(adminName)) {
            throw new IllegalArgumentException("adminName empty");
        }
        if (StringUtils.isEmpty(adminPassword)) {
            throw new IllegalArgumentException("adminPassword empty");
        }
        if (!home.isDirectory()) {
            throw new IllegalArgumentException(home.getAbsolutePath() + " is not a directory");
        }
        BaseUser admin = new BaseUser();
        admin.setName(adminName);
        admin.setPassword(adminPassword);
        admin.setHomeDirectory(home.getAbsolutePath());
        admin.setMaxIdleTime(maxIdleTimeSeconds);
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new ConcurrentLoginPermission(10, 10));
        authorities.add(new TransferRatePermission(Integer.MAX_VALUE, Integer.MAX_VALUE));
        authorities.add(new WritePermission());
        admin.setAuthorities(authorities);
        save(admin);
        this.adminName = adminName;
    }

    @Override
    public User getUserByName(String username) {
        return userMap.get(username);
    }

    @Override
    public String[] getAllUserNames() {
        return userMap.keySet().toArray(new String[userMap.size()]);
    }

    @Override
    public void delete(String username) {
        userMap.remove(username);
    }

    @Override
    public void save(User user) {
        userMap.put(user.getName(), user);
    }

    @Override
    public boolean doesExist(String username) {
        return userMap.containsKey(username);
    }

    @Override
    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
        if (authentication instanceof UsernamePasswordAuthentication) {
            UsernamePasswordAuthentication upAuth = (UsernamePasswordAuthentication) authentication;
            String userName = upAuth.getUsername();
            User user = getUserByName(userName);
            if (user == null
                    || !user.getPassword().equals(((UsernamePasswordAuthentication) authentication).getPassword())) {
                throw new AuthenticationFailedException("User + " + userName + " not known or wrong password");
            }
            return user;
        }
        throw new AuthenticationFailedException("not a named user");
    }

    @Override
    public String getAdminName() {
        return adminName;
    }

    @Override
    public boolean isAdmin(String username) {
        return adminName.equals(username);
    }
}
