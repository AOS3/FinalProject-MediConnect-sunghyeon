package com.teammeditalk.medicalconnect.ui.question.common.duration

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.teammeditalk.medicalconnect.R
import com.teammeditalk.medicalconnect.base.BaseFragment
import com.teammeditalk.medicalconnect.databinding.FragmentSelectDurationBinding
import com.teammeditalk.medicalconnect.ui.question.QuestionViewModel
import dagger.hilt.android.AndroidEntryPoint

// 5. 통증의 지속 시간
@AndroidEntryPoint
class SelectDurationFragment :
    BaseFragment<FragmentSelectDurationBinding>(
        FragmentSelectDurationBinding::inflate,
    ) {
    private val navController by lazy { findNavController() }
    private val viewModel: QuestionViewModel by activityViewModels()
    private var selectedDuration: String = ""
    private val args by navArgs<SelectDurationFragmentArgs>()

    override fun onBindLayout() {
        super.onBindLayout()

        with(binding) {
            selectBox.setOnClickListener {
                selectBox.updateSelected(true)
                selectBox2.updateSelected(false)
                selectBox3.updateSelected(false)
                selectedDuration = selectBox.getContent()
            }
            selectBox2.setOnClickListener {
                selectBox.updateSelected(false)
                selectBox2.updateSelected(true)
                selectBox3.updateSelected(false)
                selectedDuration = selectBox2.getContent()
            }
            selectBox3.setOnClickListener {
                selectBox.updateSelected(false)
                selectBox2.updateSelected(false)
                selectBox3.updateSelected(true)
                selectedDuration = selectBox3.getContent()
            }
        }
        binding.btnBack.setOnClickListener { navController.popBackStack() }
        binding.btnNext.setOnClickListener {
            when (args.hospitalType) {
                "치과" -> {
                    navController.navigate(R.id.selectDentalWorseListFragment)
                }
                "내과" -> {
                    navController.navigate(R.id.innerSymptomWorseListFragment)
                }
                "일반" -> {
                    navController.navigate(R.id.generalSymptomWorseListFragment)
                }
                "정형" -> {
                    navController.navigate(R.id.injuryHistoryFragment)
                }
                "산부" -> {
                    navController.navigate(R.id.pregnancyCheckFragment)
                }
            }
            viewModel.selectSymptomDuration(selectedDuration)
        }
    }
}
