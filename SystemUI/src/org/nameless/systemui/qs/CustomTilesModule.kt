/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.qs

import com.android.systemui.qs.tileimpl.QSTileImpl

import org.nameless.systemui.qs.tiles.CaffeineTile
import org.nameless.systemui.qs.tiles.DcDimmingTile
import org.nameless.systemui.qs.tiles.HBMTile
import org.nameless.systemui.qs.tiles.OptimizedChargeTile
import org.nameless.systemui.qs.tiles.PowerShareTile
import org.nameless.systemui.qs.tiles.QuietModeTile
import org.nameless.systemui.qs.tiles.RefreshRateTile

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
interface CustomTilesModule {

    @Binds
    @IntoMap
    @StringKey(CaffeineTile.TILE_SPEC)
    fun bindCaffeineTile(caffeineTile: CaffeineTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(DcDimmingTile.TILE_SPEC)
    fun bindDcDimmingTile(dcDimmingTile: DcDimmingTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(HBMTile.TILE_SPEC)
    fun bindHBMTile(hbmTile: HBMTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(OptimizedChargeTile.TILE_SPEC)
    fun bindOptimizedChargeTile(optimizedChargeTile: OptimizedChargeTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(PowerShareTile.TILE_SPEC)
    fun bindPowerShareTile(powerShareTile: PowerShareTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(QuietModeTile.TILE_SPEC)
    fun bindQuietModeTile(quietModeTile: QuietModeTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(RefreshRateTile.TILE_SPEC)
    fun bindRefreshRateTile(refreshRateTile: RefreshRateTile): QSTileImpl<*>
}
