package com.pds.util;

import java.util.UUID;

public final class IdGen {
    private IdGen() {}
    public static String newId() { return UUID.randomUUID().toString(); }
}
