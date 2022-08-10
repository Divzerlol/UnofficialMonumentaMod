package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.options.Options;
import ch.njol.unofficialmonumentamod.options.Options.Position;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AbiltiesHud extends HudElement {

	private static Identifier COOLDOWN_OVERLAY;
	private static Identifier COOLDOWN_FLASH;
	private static Identifier UNKNOWN_ABILITY_ICON;
	private static Identifier UNKNOWN_CLASS_BORDER;

	private String draggedAbility = null;

	public AbiltiesHud(Hud hud) {
		super(hud);
	}

	public static void registerSprites(Function<String, Identifier> register) {
		COOLDOWN_OVERLAY = register.apply("cooldown_overlay");
		COOLDOWN_FLASH = register.apply("off_cooldown");
		UNKNOWN_ABILITY_ICON = register.apply("unknown_ability");
		UNKNOWN_CLASS_BORDER = register.apply("unknown_border");
		List<Identifier> foundIcons = MinecraftClient.getInstance().getResourceManager().findResources("textures/abilities", path -> true)
			.stream().filter(id -> id.getNamespace().equals(UnofficialMonumentaModClient.MOD_IDENTIFIER)).toList();
		for (Identifier foundIcon : foundIcons) {
			if (foundIcon == COOLDOWN_OVERLAY || foundIcon == COOLDOWN_FLASH || foundIcon == UNKNOWN_ABILITY_ICON || foundIcon == UNKNOWN_CLASS_BORDER) {
				continue;
			}
			register.apply(foundIcon.getPath().substring("textures/abilities/".length(), foundIcon.getPath().length() - ".png".length()));
		}
	}

	public boolean renderInFrontOfChat() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_inFrontOfChat
			       && !(client.currentScreen instanceof ChatScreen);
	}

	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_enabled && !UnofficialMonumentaModClient.abilityHandler.abilityData.isEmpty();
	}

	@Override
	protected boolean isVisible() {
		return true;
	}

	private int getTotalSize() {
		int numAbilities = UnofficialMonumentaModClient.abilityHandler.abilityData.size();
		return UnofficialMonumentaModClient.options.abilitiesDisplay_iconSize * numAbilities + UnofficialMonumentaModClient.options.abilitiesDisplay_iconGap * (numAbilities - 1);
	}

	@Override
	protected int getWidth() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_horizontal ? getTotalSize() : UnofficialMonumentaModClient.options.abilitiesDisplay_iconSize;
	}

	@Override
	protected int getHeight() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_horizontal ? UnofficialMonumentaModClient.options.abilitiesDisplay_iconSize : getTotalSize();
	}

	@Override
	protected int getZOffset() {
		return renderInFrontOfChat() ? 100 : 0;
	}

	@Override
	protected Position getPosition() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_position;
	}

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {
		if (client.options.hudHidden || client.player == null || client.player.isSpectator()) {
			return;
		}
		Options options = UnofficialMonumentaModClient.options;

		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return;
		}

		// NB: this code is partially duplicated in ChatScreenMixin!
		// TODO fix this code duplication

		abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());

		int iconSize = options.abilitiesDisplay_iconSize;
		int iconGap = options.abilitiesDisplay_iconGap;

		boolean horizontal = options.abilitiesDisplay_horizontal;

		int totalSize = getTotalSize();

		boolean ascendingRenderOrder = options.abilitiesDisplay_ascendingRenderOrder;
		int textColor = 0xFF000000 | options.abilitiesDisplay_textColorRaw;

		float silenceCooldownFraction = abilityHandler.initialSilenceDuration <= 0 || abilityHandler.silenceDuration <= 0 ? 0 : 1f * abilityHandler.silenceDuration / abilityHandler.initialSilenceDuration;

		// multiple passes to render multiple layers.
		// layer 0: textures
		// layer 1: numbers
		for (int layer = 0; layer < 2; layer++) {

			int x = 0;
			int y = 0;
			if (!ascendingRenderOrder) {
				if (horizontal) {
					x += totalSize - iconSize;
				} else {
					y += totalSize - iconSize;
				}
			}

			for (int i = 0; i < abilityInfos.size(); i++) {
				AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(ascendingRenderOrder ? i : abilityInfos.size() - 1 - i);

				if (isAbilityVisible(abilityInfo, false)) {
					// some settings are affected by called methods, so set them anew for each ability to render
					RenderSystem.setShader(GameRenderer::getPositionTexShader);
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();

					if (layer == 0) {

						float animTicks = abilityInfo.offCooldownAnimationTicks + tickDelta;
						float animLength = 2; // half of length actually
						float scaledIconSize = iconSize * (options.abilitiesDisplay_offCooldownResize ? 1 + 0.08f * Utils.smoothStep(1 - Math.abs(animTicks - animLength) / animLength) : 1);
						float scaledX = x - (scaledIconSize - iconSize) / 2;
						float scaledY = y - (scaledIconSize - iconSize) / 2;

						drawSprite(matrices, getSpriteOrDefault(getAbilityFileIdentifier(abilityInfo.className, abilityInfo.name, abilityInfo.mode), UNKNOWN_ABILITY_ICON), scaledX, scaledY, scaledIconSize, scaledIconSize);

						// silenceCooldownFraction is >= 0 so this is also >= 0
						float cooldownFraction = abilityInfo.initialCooldown <= 0 ? 0 : Math.min(Math.max((abilityInfo.remainingCooldown - tickDelta) / abilityInfo.initialCooldown, silenceCooldownFraction), 1);
						if (cooldownFraction > 0) {
							Sprite cooldownOverlay = ModSpriteAtlasHolder.ABILITIES_ATLAS.getSprite(COOLDOWN_OVERLAY);
							float yOffset = (cooldownOverlay.getWidth() - cooldownOverlay.getHeight()) / 2f;
							drawPartialSprite(matrices, cooldownOverlay, scaledX, scaledY + yOffset, scaledIconSize, scaledIconSize - 2 * yOffset, 0, 1 - cooldownFraction, 1, 1);
						}
						if (options.abilitiesDisplay_offCooldownFlashIntensity > 0 && animTicks < 8) {
							RenderSystem.setShaderColor(1, 1, 1, options.abilitiesDisplay_offCooldownFlashIntensity * (1 - animTicks / 8f));
							drawSprite(matrices, ModSpriteAtlasHolder.ABILITIES_ATLAS.getSprite(COOLDOWN_FLASH), scaledX, scaledY, scaledIconSize, scaledIconSize);
							RenderSystem.setShaderColor(1, 1, 1, 1);
						}

						drawSprite(matrices, getSpriteOrDefault(getBorderFileIdentifier(abilityInfo.className, abilityHandler.silenceDuration > 0), UNKNOWN_CLASS_BORDER), scaledX, scaledY, scaledIconSize, scaledIconSize);

					} else {

						if ((abilityInfo.remainingCooldown > 0 || abilityHandler.silenceDuration > 0) && options.abilitiesDisplay_showCooldownAsText) {
							String cooldownString = "" + (int) Math.ceil(Math.max(Math.max(abilityInfo.remainingCooldown, abilityHandler.silenceDuration), 0) / 20f);
							drawOutlinedText(matrices, cooldownString,
								x + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.getWidth(cooldownString),
								y + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.fontHeight,
								textColor);
						}

						if (abilityInfo.maxCharges > 1 || abilityInfo.maxCharges == 1 && abilityInfo.initialCooldown <= 0) {
							drawOutlinedText(matrices, "" + abilityInfo.charges, x + options.abilitiesDisplay_textOffset, y + options.abilitiesDisplay_textOffset, textColor);
						}

					}
				}

				if (horizontal) {
					x += (ascendingRenderOrder ? 1 : -1) * (iconSize + iconGap);
				} else {
					y += (ascendingRenderOrder ? 1 : -1) * (iconSize + iconGap);
				}

			}
		}
	}

	private static final Pattern IDENTIFIER_SANITATION_PATTERN = Pattern.compile("[^a-zA-Z0-9/._-]");

	private static String sanitizeForIdentifier(String string) {
		return IDENTIFIER_SANITATION_PATTERN.matcher(string).replaceAll("_").toLowerCase(Locale.ROOT);
	}

	private static final Map<String, Identifier> abilityIdentifiers = new HashMap<>();

	private static Identifier getAbilityFileIdentifier(String className, String name, String mode) {
		return abilityIdentifiers.computeIfAbsent((className == null ? "unknown" : className) + "/" + name + (mode == null ? "" : "_" + mode),
			key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, sanitizeForIdentifier(key)));
	}

	private static final Map<String, Identifier> borderIdentifiers = new HashMap<>();

	private static Identifier getBorderFileIdentifier(String className, boolean silenced) {
		return borderIdentifiers.computeIfAbsent((className == null ? "unknown" : className) + (silenced ? "_silenced" : ""),
			key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
				sanitizeForIdentifier(className == null ? "unknown" : className) + "/border" + (silenced ? "_silenced" : "")));
	}

	private Sprite getSpriteOrDefault(Identifier identifier, Identifier defaultIdentifier) {
		Sprite sprite = ModSpriteAtlasHolder.ABILITIES_ATLAS.getSprite(identifier);
		if (sprite instanceof MissingSprite) {
			return ModSpriteAtlasHolder.ABILITIES_ATLAS.getSprite(defaultIdentifier);
		}
		return sprite;
	}

	public boolean isAbilityVisible(AbilityHandler.AbilityInfo abilityInfo, boolean forSpaceCalculation) {
		// Passive abilities are visible iff passives are enabled in the options
		if (abilityInfo.initialCooldown == 0 && abilityInfo.maxCharges == 0) {
			return UnofficialMonumentaModClient.options.abilitiesDisplay_showPassiveAbilities;
		}

		// Active abilities take up space even if hidden unless condenseOnlyOnCooldown is enabled
		if (forSpaceCalculation && !UnofficialMonumentaModClient.options.abilitiesDisplay_condenseOnlyOnCooldown) {
			return true;
		}

		// Active abilities are visible with showOnlyOnCooldown iff they are on cooldown or don't have a cooldown (and should have stacks instead)
		return !UnofficialMonumentaModClient.options.abilitiesDisplay_showOnlyOnCooldown
			       || draggedAbility != null
			       || isInEditMode()
			       || abilityInfo.remainingCooldown > 0
			       || abilityInfo.maxCharges > 0 && (abilityInfo.initialCooldown <= 0 || UnofficialMonumentaModClient.options.abilitiesDisplay_alwaysShowAbilitiesWithCharges);
	}

	@Override
	Hud.ClickResult mouseClicked(double mouseX, double mouseY, int button) {
		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return Hud.ClickResult.NONE;
		}
		abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());

		int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
		if (index < 0) {
			return Hud.ClickResult.NONE;
		}
		if (Screen.hasControlDown()) {
			return super.mouseClicked(mouseX, mouseY, button);
		} else {
			draggedAbility = abilityInfos.get(index).getOrderId();
		}
		return Hud.ClickResult.DRAG;
	}

	private int getClosestAbilityIndex(List<AbilityHandler.AbilityInfo> abilityInfos, double mouseX, double mouseY) {

		int x = 0;
		int y = 0;

		Options options = UnofficialMonumentaModClient.options;
		int iconSize = options.abilitiesDisplay_iconSize;
		int iconGap = options.abilitiesDisplay_iconGap;
		boolean horizontal = options.abilitiesDisplay_horizontal;

		int closestAbilityIndex;
		if (horizontal) {
			closestAbilityIndex = (int) Math.floor((mouseX - x + iconGap / 2.0) / (iconSize + iconGap));
		} else {
			closestAbilityIndex = (int) Math.floor((mouseY - y + iconGap / 2.0) / (iconSize + iconGap));
		}
		closestAbilityIndex = Math.max(0, Math.min(closestAbilityIndex, abilityInfos.size() - 1));

		return closestAbilityIndex;
	}

	public void renderTooltip(Screen screen, MatrixStack matrices, int mouseX, int mouseY) {
		if (!UnofficialMonumentaModClient.options.abilitiesDisplay_enabled
			    || !UnofficialMonumentaModClient.options.abilitiesDisplay_tooltips
			    || dragging
			    || draggedAbility != null) {
			return;
		}
		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return;
		}
		abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());

		int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
		if (index < 0) {
			return;
		}

		AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(index);
		screen.renderTooltip(matrices, Text.of(abilityInfo.name), mouseX, mouseY);
		// TODO also display ability description
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging) {
			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		} else if (draggedAbility != null) {
			AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
			List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
			if (abilityInfos.isEmpty()) {
				return false;
			}
			int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
			if (index < 0) {
				return false;
			}
			String abilityAtCurrentPos = abilityInfos.get(index).getOrderId();
			if (abilityAtCurrentPos.equals(draggedAbility)) {
				return false;
			}
			List<String> order = new ArrayList<>(UnofficialMonumentaModClient.options.abilitiesDisplay_order);
			int currentAbiOrderIndex = order.indexOf(abilityAtCurrentPos);
			if (currentAbiOrderIndex < 0) // shouldn't happen
			{
				return false;
			}
			order.remove(draggedAbility);
			order.add(currentAbiOrderIndex, draggedAbility);
			UnofficialMonumentaModClient.options.abilitiesDisplay_order = order;
			abilityHandler.sortAbilities();
			return true;
		}
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging) {
			super.mouseReleased(mouseX, mouseY, button);
		} else if (draggedAbility != null) {
			draggedAbility = null;
			UnofficialMonumentaModClient.saveConfig();
			return true;
		}
		return false;
	}

	public void removed() {
		super.removed();
		draggedAbility = null;
	}

	@Override
	protected boolean isClickable(double mouseX, double mouseY) {
		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return false;
		}
		abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());

		int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
		if (index < 0) {
			return false;
		}
		AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(index);
		return !isPixelTransparent(getSpriteOrDefault(getAbilityFileIdentifier(abilityInfo.className, abilityInfo.name, abilityInfo.mode), UNKNOWN_ABILITY_ICON), mouseX, mouseY)
			       || !isPixelTransparent(getSpriteOrDefault(getBorderFileIdentifier(abilityInfo.className, abilityHandler.silenceDuration > 0), UNKNOWN_CLASS_BORDER), mouseX, mouseY);
	}

}