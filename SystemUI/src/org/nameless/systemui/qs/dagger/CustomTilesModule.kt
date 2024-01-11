/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.systemui.qs.dagger

import com.android.systemui.qs.tileimpl.QSTileImpl

import org.nameless.systemui.qs.tiles.AODTile
import org.nameless.systemui.qs.tiles.AmbientDisplayTile
import org.nameless.systemui.qs.tiles.CPUInfoTile
import org.nameless.systemui.qs.tiles.CaffeineTile
import org.nameless.systemui.qs.tiles.CellularTile
import org.nameless.systemui.qs.tiles.CompassTile
import org.nameless.systemui.qs.tiles.DataSwitchTile
import org.nameless.systemui.qs.tiles.DcDimmingTile
import org.nameless.systemui.qs.tiles.HBMTile
import org.nameless.systemui.qs.tiles.HeadsUpTile
import org.nameless.systemui.qs.tiles.LocaleTile
import org.nameless.systemui.qs.tiles.OptimizedChargeTile
import org.nameless.systemui.qs.tiles.PowerShareTile
import org.nameless.systemui.qs.tiles.QuietModeTile
import org.nameless.systemui.qs.tiles.RefreshRateTile
import org.nameless.systemui.qs.tiles.SoundTile
import org.nameless.systemui.qs.tiles.SyncTile
import org.nameless.systemui.qs.tiles.SystemVibrationTile
import org.nameless.systemui.qs.tiles.UsbTetherTile
import org.nameless.systemui.qs.tiles.VpnTile
import org.nameless.systemui.qs.tiles.WifiTile

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
interface CustomTilesModule {

    @Binds
    @IntoMap
    @StringKey(AODTile.TILE_SPEC)
    fun bindAODTile(aodTile: AODTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(AmbientDisplayTile.TILE_SPEC)
    fun bindAmbientDisplayTile(ambientDisplayTile: AmbientDisplayTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(CPUInfoTile.TILE_SPEC)
    fun bindCPUInfoTile(cpuInfoTile: CPUInfoTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(CaffeineTile.TILE_SPEC)
    fun bindCaffeineTile(caffeineTile: CaffeineTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(CellularTile.TILE_SPEC)
    fun bindCellularTile(cellularTile: CellularTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(CompassTile.TILE_SPEC)
    fun bindCompassTile(compassTile: CompassTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(DataSwitchTile.TILE_SPEC)
    fun bindDataSwitchTile(dataSwitchTile: DataSwitchTile): QSTileImpl<*>

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
    @StringKey(HeadsUpTile.TILE_SPEC)
    fun bindHeadsUpTile(headsUpTile: HeadsUpTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(LocaleTile.TILE_SPEC)
    fun bindLocaleTile(localeTile: LocaleTile): QSTileImpl<*>

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

    @Binds
    @IntoMap
    @StringKey(SoundTile.TILE_SPEC)
    fun bindSoundTile(soundTile: SoundTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(SyncTile.TILE_SPEC)
    fun bindSyncTile(syncTile: SyncTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(SystemVibrationTile.TILE_SPEC)
    fun bindSystemVibrationTile(systemVibrationTile: SystemVibrationTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(UsbTetherTile.TILE_SPEC)
    fun bindUsbTetherTile(usbTetherTile: UsbTetherTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(VpnTile.TILE_SPEC)
    fun bindVpnTile(vpnTile: VpnTile): QSTileImpl<*>

    @Binds
    @IntoMap
    @StringKey(WifiTile.TILE_SPEC)
    fun bindWifiTile(wifiTile: WifiTile): QSTileImpl<*>
}
