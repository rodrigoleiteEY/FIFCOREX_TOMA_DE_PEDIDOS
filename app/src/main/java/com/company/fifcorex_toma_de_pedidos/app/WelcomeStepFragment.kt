package com.company.fifcorex_toma_de_pedidos.app

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.company.fifcorex_toma_de_pedidos.R
import com.company.fifcorex_toma_de_pedidos.databinding.ActivityWelcomeBinding
import com.sap.cloud.mobile.fiori.onboarding.LaunchScreen
import com.sap.cloud.mobile.fiori.onboarding.ext.LaunchScreenSettings
import com.sap.cloud.mobile.flowv2.core.FlowStepFragment
import com.sap.cloud.mobile.flowv2.ext.ButtonSingleClickListener
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer.getService
import com.sap.cloud.mobile.foundation.theme.ThemeDownloadService
import java.util.regex.Pattern

class WelcomeStepFragment : FlowStepFragment() {
    private lateinit var binding: ActivityWelcomeBinding
    private val logos: Pair<Bitmap?, Bitmap?> ? by lazy {
        var lightLogo: Bitmap? = null
        var darkLogo: Bitmap? = null
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = false

        getService(ThemeDownloadService::class)?.let { service ->
            service.getLightLogo()?.also { logo ->  lightLogo = BitmapFactory.decodeFile(logo.path, options)}
        }
        getService(ThemeDownloadService::class)?.let { service ->
            service.getDarkLogo()?.also { logo ->  darkLogo = BitmapFactory.decodeFile(logo.path, options)}
        }
        return@lazy Pair(lightLogo, darkLogo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityWelcomeBinding.inflate(cloneLayoutInflater(inflater), container, false)
        val settings = LaunchScreenSettings.Builder()
            .setDemoButtonVisible(false)
            .setHeaderLineLabel(getString(R.string.welcome_screen_headline_label))
            .setPrimaryButtonText(getString(R.string.welcome_screen_primary_button_label))
            .setFooterVisible(true)
            .setUrlTermsOfService("http://www.sap.com")
            .setUrlPrivacy("http://www.sap.com")
            .addInfoViewSettings(
                LaunchScreenSettings.LaunchScreenInfoViewSettings(
                    R.drawable.ic_android_white_circle_24dp,
                    getString(R.string.application_name),
                    getString(R.string.welcome_screen_detail_label)
                )
            )
            .build()
        binding.launchScreen.apply {
            initialize(settings)
            setPrimaryButtonOnClickListener(ButtonSingleClickListener{
                stepDone(R.id.stepWelcome, popCurrent = true)
            })
        }
        binding.launchScreen.findViewById<TextView>(R.id.launchscreen_description)?.also {
            Linkify.addLinks(
                it,
                Pattern.compile("SAP BTP SDK for Android"),
                null, null) {_, _ ->
                    "https://help.sap.com/doc/f53c64b93e5140918d676b927a3cd65b/Cloud/en-US/docs-en/guides/getting-started/android/overview.html"
                }
        }

        checkLogo(binding.launchScreen)
        return binding.root
    }

    private fun checkLogo(launchScreen: LaunchScreen) {
        var isDarkMode = (requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        var logoFile : Bitmap?
        if (isDarkMode){
            logoFile = logos?.second
        } else {
            logoFile = logos?.first
        }
        logoFile?.let {
            var launchScreenImages = launchScreen.findViewById<ImageView>(R.id.launchscreen_sapLogo)
            launchScreenImages.setImageBitmap(it)
        }
    }
}
