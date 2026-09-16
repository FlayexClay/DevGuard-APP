package com.devguard.shared.tenant;

import com.devguard.shared.error.TenantResolutionException;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class Tenants {

    static final Class<TenantContext> KEY = TenantContext.class;

    private Tenants() {

    }

    public static Mono<TenantContext> current() {
        return Mono.deferContextual(ctx -> ctx.hasKey(KEY)
            ? Mono.just(ctx.get(KEY))
                : Mono.error(new TenantResolutionException(
                        "No hay contexto de tenant en la peticion")));
    }

    static Context write(Context ctx, TenantContext tenant) { return ctx.put(KEY, tenant); }
}
