package org.example.model;

import java.util.HashMap;
import java.util.Map;
//a
public class UserTokenStore {

    private final Map<String, UserToken> store = new HashMap<>();

    public void save(UserToken token) {
        store.put(token.getPhone(), token);
    }

    public UserToken find(String phone) {
        return store.get(phone);
    }
}
