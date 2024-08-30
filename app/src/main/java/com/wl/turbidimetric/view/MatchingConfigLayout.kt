package com.wl.turbidimetric.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.wl.turbidimetric.R
import com.wl.turbidimetric.model.CurveModel
import com.wl.turbidimetric.model.ProjectModel
import com.wl.turbidimetric.util.FitterType
import com.wl.turbidimetric.view.dialog.isShow

class MatchingConfigLayout : FrameLayout {
    private var root: View? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, -1)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        initView()
    }

    var tabLayout: TabLayout? = null
    var mcml: MatchingConfigMatchingLayout? = null
    var mcql: MatchingConfigQualityLayout? = null
    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX)
    }

    override fun getTranslationX(): Float {
        return super.getTranslationX()
    }

    init {

    }

    private fun listenerView() {

    }


    fun initView() {
        root = LayoutInflater.from(context).inflate(R.layout.layout_matching_config, this, true)
        tabLayout = root?.findViewById(R.id.tabLayout)
        mcml = root?.findViewById(R.id.mcml)
        mcql = root?.findViewById(R.id.mcql)

        tabLayout?.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                matchingType = if (tab!!.position == 0) {
                    MatchingType.Matching
                } else {
                    MatchingType.Quality
                }
                changeMatchingType(matchingType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    enum class MatchingType {
        Matching, Quality
    }

    fun changeMatchingType(matchingType: MatchingType) {
        this.matchingType = matchingType
        mcml?.visibility = (matchingType == MatchingType.Matching).isShow()
        mcql?.visibility = (matchingType == MatchingType.Quality).isShow()

        setContent()
    }

    var matchingType: MatchingType = MatchingType.Matching
    var curves: List<CurveModel> = mutableListOf()
    var reagentNo: String = ""
    var quality: Boolean = false
    var projects: List<ProjectModel>? = null
    var autoAttenuation: Boolean = false
    var gradsNum: Int = 5
    var selectProject: ProjectModel? = null
    var selectFitterType: FitterType = FitterType.Three
    var targetCons: List<Double> = mutableListOf()


    var qualityLow1: Int = 0
    var qualityLow2: Int = 0
    var qualityHigh1: Int = 0
    var qualityHigh2: Int = 0
    var selectCurve: CurveModel? = null
    fun updateContent(
        matchingType: MatchingType,
        curves: List<CurveModel>,
        qualityLow1: Int,
        qualityLow2: Int,
        qualityHigh1: Int,
        qualityHigh2: Int,
        reagentNo: String = "",
        quality: Boolean = false,
        projects: List<ProjectModel>,
        autoAttenuation: Boolean,
        gradsNum: Int = 5,
        selectProject: ProjectModel? = null,
        selectFitterType: FitterType = FitterType.Three,
        targetCons: List<Double> = mutableListOf()
    ) {

        this.matchingType = matchingType
        this.curves = curves
        this.qualityLow1 = qualityLow1
        this.qualityLow2 = qualityLow2
        this.qualityHigh1 = qualityHigh1
        this.qualityHigh2 = qualityHigh2
        this.reagentNo = reagentNo
        this.quality = quality
        this.projects = projects
        this.autoAttenuation = autoAttenuation
        this.gradsNum = gradsNum
        this.selectProject = selectProject
        this.selectFitterType = selectFitterType
        this.targetCons = targetCons
        this.setContent()
    }

    fun getCurInput() {
        if (matchingType == MatchingType.Matching) {

            mcml?.let { mcml ->
                mcml.getCurInput()
                targetCons = mcml.cons
                reagentNo = mcml.reagentNoStr
                qualityLow1 = mcml.qualityLow1
                qualityLow2 = mcml.qualityLow2
                qualityHigh1 = mcml.qualityHigh1
                qualityHigh2 = mcml.qualityHigh2
                selectProject = mcml.selectProject
                quality = mcml.quality
                gradsNum = mcml.gradsNum
                autoAttenuation = mcml.autoAttenuation
                selectFitterType = mcml.selectFitterType
            }

        } else {
            mcql?.let { mcql ->
                mcql.getCurInput()
                qualityLow1 = mcql.qualityLow1
                qualityLow2 = mcql.qualityLow2
                qualityHigh1 = mcql.qualityHigh1
                qualityHigh2 = mcql.qualityHigh2
                selectCurve = mcql.selectCurve
                quality = true
            }
        }
    }

    fun setContent() {
        if (matchingType == MatchingType.Matching) {
            tabLayout?.let { tabView ->
                tabView?.selectTab(tabView.getTabAt(0))
            }
            mcml?.updateContent(
                qualityLow1,
                qualityLow2,
                qualityHigh1,
                qualityHigh2,
                reagentNo,
                quality,
                projects ?: mutableListOf(),
                autoAttenuation,
                gradsNum,
                selectProject,
                selectFitterType,
                targetCons
            )
        } else {
            tabLayout?.let { tabView ->
                tabView?.selectTab(tabView.getTabAt(1))
            }
            mcql?.updateContent(
                selectCurve,
                curves,
                qualityLow1,
                qualityLow2,
                qualityHigh1,
                qualityHigh2,
            )
        }
        mcml?.visibility = (matchingType == MatchingType.Matching).isShow()
        mcql?.visibility = (matchingType == MatchingType.Quality).isShow()
    }

}
