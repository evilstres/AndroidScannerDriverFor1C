package com.ptolkachev.androidscanner

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.ClipboardManager.OnPrimaryClipChangedListener
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class Scanner(var mActivity: Activity, var mCallObject: Long) : Runnable,
	OnPrimaryClipChangedListener {
	var mScanMode = "broadcast"
	var mExtraData: String? = null
	var mReceiver: BroadcastReceiver? = null
	fun show() {
		mActivity.runOnUiThread(this)
	}

	override fun run() {
		System.loadLibrary("com_ptolkachev_AndroidScanner")
	}

	fun start(scanMode: String, actionName: String?, extraData: String?) {
		if (mReceiver != null) {
			return
		}
		mScanMode = scanMode
		if (mScanMode == "clipboard") {
			this.clipboardManager?.addPrimaryClipChangedListener(this)
		} else {
			mExtraData = extraData
			mReceiver = object : BroadcastReceiver() {
				override fun onReceive(context: Context, intent: Intent?) {
					if (intent != null && intent.hasExtra(mExtraData)) {
						val barCode = intent.getStringExtra(mExtraData)
						if (!barCode.isNullOrEmpty()) {
							OnBarcodeReceived(mCallObject, barCode)
						}
					}
				}
			}
			val filter = IntentFilter(actionName)
			mActivity.registerReceiver(mReceiver, filter)
		}
	}

	fun stop() {
		if (mReceiver != null) {
			mActivity.unregisterReceiver(mReceiver)
			mReceiver = null
		}
		if (mScanMode == "clipboard") {
			this.clipboardManager?.removePrimaryClipChangedListener(this)
		}
	}

	override fun onPrimaryClipChanged() {
		val clipboardManager = clipboardManager
		if (clipboardManager != null) {
			val clipData = clipboardManager.primaryClip
			if (clipData != null) {
				val item = clipData.getItemAt(0)
				val barCode = item.text.toString()
				if (barCode.isNotEmpty()) {
					OnBarcodeReceived(mCallObject, barCode)
				}
			}
		}
	}

	private val clipboardManager: ClipboardManager?
		get() = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?

	companion object {
		// in C/C++ code the function will have name
		// Java_com_ptolkachev_androidscanner_Scanner_OnBarcodeReceived
		external fun OnBarcodeReceived(pObject: Long, barcode: String?)
	}
}