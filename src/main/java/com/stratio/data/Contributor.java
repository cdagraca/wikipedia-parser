/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.data;

import static com.google.common.base.Preconditions.*;
import com.google.common.net.InetAddresses;

/**
 * A Mediawiki contributor.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class Contributor {

    private Integer id;
    private String username;
    private Boolean isAnonymous;

    public Contributor(int id, String username) {
        this(id, checkNotNull(username),
                username.length() >= 7 &&
                username.length() <= 15 &&
                InetAddresses.isInetAddress(username)
                );
    }

    public Contributor(int id, String username, boolean isAnonymous) {
        this.id = id;
        this.username = checkNotNull(username);
        this.isAnonymous = isAnonymous;
    }

    public String getUsername() {
        return username;
    }

    public Integer getId() {
        return id;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    @Override
    public String toString() {
        return "Contributor{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", isAnonymous=" + isAnonymous +
                '}';
    }
}
