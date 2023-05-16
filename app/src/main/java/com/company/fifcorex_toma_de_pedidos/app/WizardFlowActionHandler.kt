package com.company.fifcorex_toma_de_pedidos.app

import com.company.fifcorex_toma_de_pedidos.R
import com.sap.cloud.mobile.flowv2.ext.FlowActionHandler
import com.sap.cloud.mobile.flowv2.ext.FlowCustomStep
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.foundation.authentication.CertificateProvider
import com.sap.cloud.mobile.foundation.authentication.SystemCertificateProvider

class WizardFlowActionHandler(val application: SAPWizardApplication): FlowActionHandler() {


    override fun getCertificateProvider(): CertificateProvider {
        return SystemCertificateProvider()
    }

    override fun getFlowCustomizationStep(runningFlowName: String?): List<FlowCustomStep<*>> {
        return if (runningFlowName == FlowType.ONBOARDING.name) {
            listOf(
                    FlowCustomStep.BeforeEula(R.id.stepWelcome, WelcomeStepFragment::class)
            )
        } else super.getFlowCustomizationStep(runningFlowName)
    }
}
