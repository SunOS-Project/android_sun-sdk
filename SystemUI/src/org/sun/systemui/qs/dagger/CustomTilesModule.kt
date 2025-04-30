/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.sun.systemui.qs.dagger

import com.android.systemui.qs.tileimpl.QSTileImpl

import org.sun.systemui.qs.tiles.AODTile
import org.sun.systemui.qs.tiles.CPUInfoTile
import org.sun.systemui.qs.tiles.CaffeineTile
import org.sun.systemui.qs.tiles.CellularTile
import org.sun.systemui.qs.tiles.CompassTile
import org.sun.systemui.qs.tiles.DataSwitchTile
import org.sun.systemui.qs.tiles.DcDimmingTile
import org.sun.systemui.qs.tiles.HBMTile
import org.sun.systemui.qs.tiles.HeadsUpTile
import org.sun.systemui.qs.tiles.LocaleTile
import org.sun.systemui.qs.tiles.NrTile
import org.sun.systemui.qs.tiles.OptimizedChargeTile
import org.sun.systemui.qs.tiles.PowerShareTile
import org.sun.systemui.qs.tiles.QuietModeTile
import org.sun.systemui.qs.tiles.RefreshRateTile
import org.sun.systemui.qs.tiles.ScreenshotTile
import org.sun.systemui.qs.tiles.SoundTile
import org.sun.systemui.qs.tiles.SyncTile
import org.sun.systemui.qs.tiles.SystemVibrationTile
import org.sun.systemui.qs.tiles.UsbTetherTile
import org.sun.systemui.qs.tiles.VpnTile
import org.sun.systemui.qs.tiles.WifiTile

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
    @StringKey(NrTile.TILE_SPEC)
    fun bindNrTile(nrTile: NrTile): QSTileImpl<*>

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
    @StringKey(ScreenshotTile.TILE_SPEC)
    fun bindScreenshotTile(screenshotTile: ScreenshotTile): QSTileImpl<*>

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
