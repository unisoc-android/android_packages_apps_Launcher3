package com.sprd.ext.unreadnotifier;

import com.android.launcher3.util.ComponentKey;

class UnreadKeyData {
    ComponentKey componentKey;
    int unreadNum;

    UnreadKeyData(ComponentKey key, int num) {
        componentKey = key;
        unreadNum = num;
    }
}
