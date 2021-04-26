package dev.ftb.mods.ftbteams.client;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbguilibrary.icon.FaceIcon;
import dev.ftb.mods.ftbguilibrary.utils.MouseButton;
import dev.ftb.mods.ftbguilibrary.widget.BaseScreen;
import dev.ftb.mods.ftbguilibrary.widget.NordButton;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbguilibrary.widget.WidgetLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class PlayerListScreen extends BaseScreen {
	public final Component title;
	public final List<GameProfile> profiles;
	public final Set<GameProfile> selected;
	public final Consumer<GameProfile> callback;

	public PlayerListScreen(Component t, List<GameProfile> p, Consumer<GameProfile> c) {
		title = t;
		profiles = p;
		selected = new HashSet<>();
		callback = c;
	}

	@Override
	public void addWidgets() {
		add(new Panel(this) {
			@Override
			public void addWidgets() {
				for (GameProfile profile : profiles) {
					add(new NordButton(this, new TextComponent(profile.getName()), FaceIcon.getFace(profile)) {
						@Override
						public void onClicked(MouseButton mouseButton) {
							if (selected.contains(profile)) {
								selected.remove(profile);
							} else {
								selected.add(profile);
							}

							refreshWidgets();
						}
					});
				}
			}

			@Override
			public void alignWidgets() {
				align(new WidgetLayout.Vertical(1, 2, 1));
			}
		}.setPosAndSize(4, 12, width - 8, height - 16));
	}
}
