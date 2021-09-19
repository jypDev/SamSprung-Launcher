package com.sec.android.app.shealth

/* ====================================================================
 * Copyright (c) 2012-2021 Abandoned Cart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by Abandoned Cart" unless
 *    otherwise displayed by tagged, public repository entries.
 *
 * 4. The names "8-Bit Dream", "TwistedUmbrella" and "Abandoned Cart"
 *    must not be used in any form to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called "8-Bit Dream",
 *    "TwistedUmbrella" or "Abandoned Cart" nor may these labels appear
 *    in their names without prior written permission of Abandoned Cart.
 *
 * THIS SOFTWARE IS PROVIDED BY Abandoned Cart ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.util.*

class AppLauncherService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StepRemoteViewsFactory(applicationContext)
    }

    class StepRemoteViewsFactory(private val context: Context) : RemoteViewsFactory {
        private val hidden = "hidden_packages"
        private var isGridView = true
        private var packages: MutableList<ResolveInfo> = arrayListOf()
        private val pacMan = context.packageManager
        private lateinit var mainIntent: Intent
        private val mReceiver: BroadcastReceiver = OffBroadcastReceiver()

        override fun onCreate() {
            mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            mainIntent.removeCategory(Intent.CATEGORY_HOME)

            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }.also {
                SamSprung.context.registerReceiver(mReceiver, it)
            }
        }
        override fun onDataSetChanged() {
            isGridView = SamSprung.prefs.getBoolean("gridview", isGridView)

            packages = pacMan.queryIntentActivities(mainIntent, 0)
            packages.removeIf { item -> SamSprung.prefs.getStringSet(
                hidden, HashSet())!!.contains(item.activityInfo.packageName) }
            Collections.sort(packages, ResolveInfo.DisplayNameComparator(pacMan))
        }
        override fun onDestroy() {
            SamSprung.context.unregisterReceiver(mReceiver)
            packages.clear()
        }

        override fun getCount(): Int {
            return packages.size
        }

        override fun getViewAt(position: Int): RemoteViews {
            val application = packages[position]
            val rv = RemoteViews(context.packageName, R.layout.step_widget_item)

            rv.setViewVisibility(
                R.id.widgetListContainer,
                if (isGridView) View.GONE else View.VISIBLE)
            rv.setViewVisibility(
                R.id.widgetGridImage,
                if (isGridView) View.VISIBLE else View.GONE)

            val icon = if (isGridView) R.id.widgetGridImage else R.id.widgetItemImage

            rv.setImageViewBitmap(
                icon, getBitmapFromDrawable(application.loadIcon(pacMan))
            )
            if (!isGridView) {
                rv.setTextViewText(R.id.widgetItemText,
                    application.loadLabel(pacMan).toString())
            }

            val extras = Bundle()
            extras.putString("launchPackage", application.activityInfo.packageName)
            extras.putString("launchActivity", application.activityInfo.name)
            val fillInIntent = Intent()
            fillInIntent.putExtras(extras)
                rv.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent)
            return rv
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
            val bitmapDrawable = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmapDrawable)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmapDrawable
        }
    }
}