package com.revolgenx.anilib.character.fragment

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import com.revolgenx.anilib.R
import com.revolgenx.anilib.character.data.model.CharacterModel
import com.revolgenx.anilib.common.ui.fragment.BaseLayoutFragment
import com.revolgenx.anilib.databinding.CharacterFragmentLayoutBinding
import com.revolgenx.anilib.social.factory.AlMarkwonFactory
import com.revolgenx.anilib.ui.view.makeToast
import com.revolgenx.anilib.util.prettyNumberFormat
import com.revolgenx.anilib.character.viewmodel.CharacterViewModel
import com.revolgenx.anilib.common.event.OpenImageEvent
import com.revolgenx.anilib.common.repository.util.Resource
import com.revolgenx.anilib.util.loginContinue
import org.koin.androidx.viewmodel.ext.android.viewModel

class CharacterFragment : BaseLayoutFragment<CharacterFragmentLayoutBinding>() {
    private val viewModel by viewModel<CharacterViewModel>()
    private var characterModel: CharacterModel? = null
    private val characterId: Int? get() = arguments?.getInt(CHARACTER_ID_KEY)

    companion object {
        private const val CHARACTER_ID_KEY = "CHARACTER_ID_KEY"
        fun newInstance(characterId: Int) = CharacterFragment().also {
            it.arguments = bundleOf(CHARACTER_ID_KEY to characterId)
        }
    }

    override fun bindView(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): CharacterFragmentLayoutBinding {
        return CharacterFragmentLayoutBinding.inflate(inflater, parent, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.field.characterId = characterId ?: return
        viewModel.toggleField.characterId = characterId

        viewModel.characterInfoLiveData.observe(viewLifecycleOwner) { res ->
            val resourceLayout = binding.resourceStatusLayout
            when (res) {
                is Resource.Success -> {
                    resourceLayout.resourceStatusContainer.visibility = View.GONE
                    resourceLayout.resourceProgressLayout.progressLayout.visibility = View.VISIBLE
                    resourceLayout.resourceErrorLayout.errorLayout.visibility = View.GONE
                    binding.updateView(res.data!!)
                }
                is Resource.Error -> {
                    resourceLayout.resourceStatusContainer.visibility = View.VISIBLE
                    resourceLayout.resourceProgressLayout.progressLayout.visibility = View.GONE
                    resourceLayout.resourceErrorLayout.errorLayout.visibility = View.VISIBLE
                }
                is Resource.Loading -> {
                    resourceLayout.resourceStatusContainer.visibility = View.VISIBLE
                    resourceLayout.resourceProgressLayout.progressLayout.visibility = View.VISIBLE
                    resourceLayout.resourceErrorLayout.errorLayout.visibility = View.GONE
                }
            }
        }

        viewModel.toggleFavouriteLiveData.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    binding.characterFavIv.showLoading(false)
                    if (res.data == true) {
                        characterModel?.isFavourite = characterModel?.isFavourite?.not() ?: false
                        binding.characterFavIv.setImageResource(
                            if (characterModel?.isFavourite == true) {
                                R.drawable.ic_favourite
                            } else {
                                R.drawable.ic_not_favourite
                            }
                        )
                    }
                }
                is Resource.Error -> {
                    binding.characterFavIv.showLoading(false)
                    makeToast(R.string.failed_to_toggle, icon = R.drawable.ic_error)
                }
                is Resource.Loading -> {
                    binding.characterFavIv.showLoading(true)
                }
            }
        }

        initListener()

        if (savedInstanceState == null) {
            viewModel.getCharacterInfo(viewModel.field)
        }
    }

    override fun updateToolbar() {
        val model = characterModel ?: return
        (parentFragment as? CharacterContainerFragment)?.let {
            it.updateToolbarTitle(model.name!!.full!!)
            it.updateShareableLink(model.siteUrl)
        }
    }

    private fun initListener() {
        binding.characterFavLayout.setOnClickListener {
            loginContinue {
                viewModel.toggleCharacterFav(viewModel.toggleField)
            }
        }
    }

    private fun CharacterFragmentLayoutBinding.updateView(item: CharacterModel) {
        characterModel = item
        updateToolbar()
        characterNameTv.text = item.name?.full
        characterFavCountTv.text = item.favourites?.toLong()?.prettyNumberFormat()
        characterIv.setImageURI(characterModel?.image?.image)
        characterIv.setOnClickListener {
            characterModel?.image?.image?.let {
                OpenImageEvent(it).postEvent
            }
        }
        item.name?.native?.let {
            nativeNameTv.subtitle = it
        } ?: let {
            nativeNameTv.visibility = View.GONE
        }

        item.name?.alternative?.takeIf { it.isNullOrEmpty().not() }?.let {
            alternativeNameTv.subtitle = it.joinToString()
        } ?: let {
            alternativeNameTv.visibility = View.GONE
        }

        if (item.isFavourite) {
            characterFavIv.setImageResource(R.drawable.ic_favourite)
        }

        AlMarkwonFactory.getMarkwon()
            .setMarkdown(characterDescriptionTv, item.description ?: "")
    }
}