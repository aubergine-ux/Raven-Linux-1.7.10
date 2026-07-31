package keystrokesmod.client.clickgui.raven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import keystrokesmod.client.clickgui.raven.components.CategoryComponent;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;
import keystrokesmod.client.module.modules.client.GuiModule;
import keystrokesmod.client.utils.RenderUtils;
import keystrokesmod.client.utils.Timer;
import keystrokesmod.client.utils.Utils;
import keystrokesmod.client.utils.ParticleSystem;
import keystrokesmod.client.utils.font.FontUtil;
import keystrokesmod.client.utils.version.Version;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ClickGui extends GuiScreen {
    private ScheduledFuture<?> sf;
    private Timer aT, aL, aE, aR;
    private final ArrayList<CategoryComponent> categoryList;
    private CategoryComponent lastCategory;
    public static int mouseX, mouseY;
    public final Terminal terminal;
    private ParticleSystem particleSystem;

    public static int binding;
    private ResourceLocation animeGirlLocation;
    
    // Animation and drag variables
    private float openAnimation = 0f;
    private long openStartTime = 0;
    private boolean isDragging = false;
    private CategoryComponent draggedCategory = null;
    private int dragOffsetX, dragOffsetY;
    private float categoryAnimations[] = new float[Module.ModuleCategory.values().length];
    
    // Quicksearch variables
    private String searchQuery = "";
    private long lastSearchTime = 0;
    private static final long SEARCH_TIMEOUT = 2000; // 2 seconds timeout

    public ClickGui() {
        animeGirlLocation = RenderUtils.getResourcePath("/assets/keystrokesmod/animestuff/animegirl.png");
        this.terminal = new Terminal();
        this.categoryList = new ArrayList<>();
        this.openStartTime = System.currentTimeMillis();
        this.particleSystem = new ParticleSystem(100);
        
        Module.ModuleCategory[] values;
        int categoryAmount = (values = Module.ModuleCategory.values()).length;
        int xOffSet = 5;
        int yOffSet = 5;
        for (int category = 0; category < categoryAmount; ++category) {
            Module.ModuleCategory moduleCategory = values[category];
            CategoryComponent currentModuleCategory = new CategoryComponent(moduleCategory);
            currentModuleCategory.visable = (currentModuleCategory.categoryName.isShownByDefault());
            categoryList.add(currentModuleCategory);
            currentModuleCategory.setCoords(xOffSet, yOffSet);
            xOffSet = xOffSet + 100;
            if (xOffSet > 400) {
                xOffSet = 5;
                yOffSet += 120;
            }
            
            // Initialize category animations
            if (category < categoryAnimations.length) {
                categoryAnimations[category] = 0f;
            }
        }
        terminal.setLocation(380, 0);
        terminal.setSize((int) (92 * 1.5), (int) ((92 * 1.5) * 0.75));
    }

    public void initMain() {
        (this.aT = this.aE = this.aR = new Timer(500.0F)).start();
        this.sf = Raven.getExecutor().schedule(() -> (this.aL = new Timer(650.0F)).start(), 650L,
                TimeUnit.MILLISECONDS);
    }

    @Override
	public void initGui() {
        super.initGui();
        categoryList.forEach(CategoryComponent::initGui);
    }

    @Override
	public void drawScreen(int x, int y, float p) {
    	super.drawScreen(x, y, p);
    	mouseX = x; mouseY = y;
        Version clientVersion = Raven.versionManager.getClientVersion();
        Version latestVersion = Raven.versionManager.getLatestVersion();
        
        // Calculate opening animation
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - openStartTime) / 1000f;
        openAnimation = Math.min(1f, deltaTime * 2.5f); // 2.5 second ease-in animation

        // Enhanced background with gradient effect and animation
        int bgColor = (int) (this.aR.getValueFloat(0.0F, 0.8F, 2) * 255.0F * openAnimation) << 24;
        drawRect(0, 0, this.width, this.height, bgColor);

        // Draw particles
        particleSystem.render(mouseX, mouseY, this.width, this.height, openAnimation);
        
        // Add subtle gradient overlay with animation
        for (int i = 0; i < 5; i++) {
            int alpha = (int) (20 * (1 - i * 0.2) * openAnimation);
            int gradientColor = (alpha << 24) | (0x1A << 16) | (0x1A << 8) | 0x1A;
            drawRect(0, i * (this.height / 5), this.width, (i + 1) * (this.height / 5), gradientColor);
        }
        
        // Enhanced anime girl background with glow effect (only if enabled)
        if (GuiModule.animeGirlEnabled()) {
            // Use customizable size from GUI module
            int animeSize = (int) GuiModule.getAnimeGirlSize();
            int animeWidth = animeSize;
            int animeHeight = animeSize;
            int animeX = 5;
            int animeY = this.height - animeHeight - 5;
            
            // Add glow effect behind anime girl
            GL11.glEnable(GL11.GL_BLEND);
            for (int i = 3; i >= 0; i--) {
                int glowAlpha = (int) ((15 - i * 4) * (GuiModule.getAnimeGirlOpacity() / 100.0f));
                int glowSize = animeSize + i * 10;
                GL11.glColor4f(1.0F, 0.5F, 0.8F, glowAlpha / 255.0F);
                drawRect(animeX - i * 5, animeY - i * 5, animeX + glowSize, animeY + glowSize, 
                        (glowAlpha << 24) | (255 << 16) | (128 << 8) | 204);
            }
            
            // Use customizable opacity from GUI module
            float opacity = GuiModule.getAnimeGirlOpacity() / 100.0f;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, opacity);
            this.mc.getTextureManager().bindTexture(animeGirlLocation);
            RenderUtils.drawModalRectWithCustomSizedTexture(animeX, animeY, 0, 0, animeWidth, animeHeight, animeWidth, animeHeight);
            GL11.glDisable(GL11.GL_BLEND);
        }
        
        int quarterScreenHeight = this.height / 4;
        int halfScreenWidth = this.width / 2;
        int w_c = 30 - this.aT.getValueInt(0, 30, 3);
        
        // Enhanced logo animation with floating effect and opening animation
        float floatOffset = (float) Math.sin(currentTime * 0.002) * 3;
        int logoY = quarterScreenHeight + (int) floatOffset;
        
        // Apply opening animation to logo scale and position
        float logoScale = 0.5f + (1f - 0.5f) * openAnimation;
        int logoOffsetY = (int) ((1 - openAnimation) * 50);
        logoY += logoOffsetY;
        
        // Draw logo with shadow effect and animation
        for (int i = 2; i >= 0; i--) {
            int shadowAlpha = (int) ((60 - i * 20) * openAnimation);
            int shadowOffset = i + 1;
            this.drawCenteredString(this.fontRendererObj, "r", (halfScreenWidth + 1) - w_c + shadowOffset, 
                    logoY - 25 + shadowOffset, (shadowAlpha << 24));
            this.drawCenteredString(this.fontRendererObj, "a", halfScreenWidth - w_c + shadowOffset, 
                    logoY - 15 + shadowOffset, (shadowAlpha << 24));
            this.drawCenteredString(this.fontRendererObj, "v", halfScreenWidth - w_c + shadowOffset, 
                    logoY - 5 + shadowOffset, (shadowAlpha << 24));
            this.drawCenteredString(this.fontRendererObj, "e", halfScreenWidth - w_c + shadowOffset, 
                    logoY + 5 + shadowOffset, (shadowAlpha << 24));
            this.drawCenteredString(this.fontRendererObj, "n", halfScreenWidth - w_c + shadowOffset, 
                    logoY + 15 + shadowOffset, (shadowAlpha << 24));
            this.drawCenteredString(this.fontRendererObj, "b", halfScreenWidth + 1 + w_c + shadowOffset, 
                    logoY + 25 + shadowOffset, (shadowAlpha << 24));
        }
        
        // Main logo with enhanced rainbow effect and animation
        if (openAnimation > 0.1f) {
            this.drawCenteredString(this.fontRendererObj, "r", (halfScreenWidth + 1) - w_c, logoY - 25,
                    Utils.Client.rainbowDraw(2L, 1500L));
            this.drawCenteredString(this.fontRendererObj, "a", halfScreenWidth - w_c, logoY - 15,
                    Utils.Client.rainbowDraw(2L, 1200L));
            this.drawCenteredString(this.fontRendererObj, "v", halfScreenWidth - w_c, logoY - 5,
                    Utils.Client.rainbowDraw(2L, 900L));
            this.drawCenteredString(this.fontRendererObj, "e", halfScreenWidth - w_c, logoY + 5,
                    Utils.Client.rainbowDraw(2L, 600L));
            this.drawCenteredString(this.fontRendererObj, "n", halfScreenWidth - w_c, logoY + 15,
                    Utils.Client.rainbowDraw(2L, 300L));
            this.drawCenteredString(this.fontRendererObj, "b", halfScreenWidth + 1 + w_c, logoY + 25,
                    Utils.Client.rainbowDraw(2L, 0L));
            this.drawCenteredString(this.fontRendererObj, "+ +", halfScreenWidth + 1 + w_c, logoY + 30,
                    Utils.Client.rainbowDraw(2L, 0L));
        }

        float speed = 4890;

        // Enhanced version display with background
        if (latestVersion.isNewerThan(clientVersion)) {
            // Draw update notification background
            int updateBoxWidth = 300;
            int updateBoxHeight = 40;
            int updateBoxX = halfScreenWidth - updateBoxWidth / 2;
            int updateBoxY = this.height - 50;
            
            RenderUtils.drawRoundedRect(updateBoxX, updateBoxY, updateBoxX + updateBoxWidth, 
                    updateBoxY + updateBoxHeight, 5, 0x40FF6B6B);
            RenderUtils.drawBorderedRoundedRect(updateBoxX, updateBoxY, updateBoxX + updateBoxWidth, 
                    updateBoxY + updateBoxHeight, 5, 2, 0xFFFF6B6B, 0x40FF6B6B);
            
            int margin = 2;
            int rows = 1;
            for (int i = Raven.updateText.length - 1; i >= 0; i--) {
                String up = Raven.updateText[i];
                if (GuiModule.useCustomFont())
					FontUtil.normal.drawSmoothString(up, halfScreenWidth - (this.fontRendererObj.getStringWidth(up) / 2),
                            updateBoxY + 8 + (rows - 1) * 12,
                            Utils.Client.astolfoColorsDraw(10, 28, speed));
				else
					mc.fontRendererObj.drawStringWithShadow(up,
                            halfScreenWidth - (this.fontRendererObj.getStringWidth(up) / 2),
                            updateBoxY + 8 + (rows - 1) * 12,
                            Utils.Client.astolfoColorsDraw(10, 28, speed));
                rows++;
                margin += 2;
            }
        } else {
            // Normal version display with subtle background
            int versionBoxWidth = 400;
            int versionBoxHeight = 20;
            int versionBoxX = 4;
            int versionBoxY = this.height - 25;
            
            RenderUtils.drawRoundedRect(versionBoxX, versionBoxY, versionBoxX + versionBoxWidth, 
                    versionBoxY + versionBoxHeight, 3, 0x40202020);
            
            if (GuiModule.useCustomFont())
				FontUtil.normal.drawSmoothString(
			            "Raven B++ v" + clientVersion + " | Config: " + Raven.configManager.getConfig().getName(),
			            versionBoxX + 5, versionBoxY + 6,
			            Utils.Client.astolfoColorsDraw(10, 14, speed));
			else
				mc.fontRendererObj.drawStringWithShadow(
			            "Raven B++ v" + clientVersion + " | Config: " + Raven.configManager.getConfig().getName(),
			            versionBoxX + 5, versionBoxY + 6,
			            Utils.Client.astolfoColorsDraw(10, 14, speed));
        }

        // Enhanced decorative elements with glow
        int glowColor = Utils.Client.customDraw(0);
        for (int i = 0; i < 2; i++) {
            int alpha = 30 - i * 15;
            int offset = i + 1;
            this.drawVerticalLine(halfScreenWidth - 10 - w_c - offset, logoY - 30, logoY + 38,
                    (alpha << 24) | (glowColor & 0xFFFFFF));
            this.drawVerticalLine(halfScreenWidth + 10 + w_c + offset, logoY - 30, logoY + 38,
                    (alpha << 24) | (glowColor & 0xFFFFFF));
        }
        
        this.drawVerticalLine(halfScreenWidth - 10 - w_c, logoY - 30, logoY + 38, glowColor);
        this.drawVerticalLine(halfScreenWidth + 10 + w_c, logoY - 30, logoY + 38, glowColor);
        
        int animationProggress;
        if (this.aL != null) {
            animationProggress = this.aL.getValueInt(0, 20, 2);
            // Enhanced horizontal lines with glow
            for (int i = 0; i < 2; i++) {
                int alpha = 40 - i * 20;
                int offset = i;
                this.drawHorizontalLine(halfScreenWidth - 10 - offset, (halfScreenWidth - 10) + animationProggress,
                        logoY - 29 - offset, (alpha << 24) | (glowColor & 0xFFFFFF));
                this.drawHorizontalLine(halfScreenWidth + 10 + offset, (halfScreenWidth + 10) - animationProggress,
                        logoY + 38 + offset, (alpha << 24) | (glowColor & 0xFFFFFF));
            }
            this.drawHorizontalLine(halfScreenWidth - 10, (halfScreenWidth - 10) + animationProggress,
                    logoY - 29, glowColor);
            this.drawHorizontalLine(halfScreenWidth + 10, (halfScreenWidth + 10) - animationProggress,
                    logoY + 38, glowColor);
        }

        // Enhanced category rendering with staggered animations
        ArrayList<CategoryComponent> visibleCategories = visableCategoryList();
        for (int i = 0; i < visibleCategories.size(); i++) {
            CategoryComponent category = visibleCategories.get(i);
            
            // Calculate staggered animation for each category
            float categoryDelay = i * 0.1f;
            float categoryAnimation = Math.max(0f, Math.min(1f, (openAnimation - categoryDelay) * 2f));
            
            if (categoryAnimation > 0) {
                // Apply animation to category position and alpha
                int originalX = category.getX();
                int originalY = category.getY();
                
                // Add slide-in animation
                int slideOffset = (int) ((1 - categoryAnimation) * -50);
                category.setCoords(originalX + slideOffset, originalY);
                
                // Draw with alpha
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(1f, 1f, 1f, categoryAnimation);
                category.draw(x, y);
                GL11.glDisable(GL11.GL_BLEND);
                
                // Restore original position
                category.setCoords(originalX, originalY);
            }
        }

        // Enhanced player display with rotation and animation
        if (openAnimation > 0.5f && this.mc.thePlayer != null) {
            GL11.glPushMatrix();
            GL11.glColor4f(1f, 1f, 1f, (openAnimation - 0.5f) * 2f);
            
            // Calculate position with animation
            int playerX = (this.width + 15) - this.aE.getValueInt(0, 40, 2);
            int playerY = this.height - 19 - this.fontRendererObj.FONT_HEIGHT;
            
            // Add subtle rotation effect
            float rotation = (float) (currentTime * 0.001);
            
            try {
                GuiInventory.drawEntityOnScreen(playerX, playerY, 40, 
                        (float) (this.width - 25 - x), 
                        (float) (this.height - 50 - y), 
                        this.mc.thePlayer);
            } catch (Exception e) {
                // Fallback if player rendering fails
                GL11.glPopMatrix();
                return;
            }
            
            GL11.glPopMatrix();
        }

        terminal.update(x, y);
        if (openAnimation > 0.7f) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, (openAnimation - 0.7f) / 0.3f);
            terminal.draw();
            GL11.glDisable(GL11.GL_BLEND);
        }
        
        // Draw search query if active
        if (!searchQuery.isEmpty()) {
            long timeSinceSearch = System.currentTimeMillis() - lastSearchTime;
            if (timeSinceSearch < SEARCH_TIMEOUT) {
                float searchAlpha = Math.max(0f, 1f - (timeSinceSearch / (float) SEARCH_TIMEOUT));
                int searchColor = (int) (255 * searchAlpha) << 24 | 0xFFFF8C00;
                
                int padding = 6;
                int textWidth = (int) FontUtil.normal.getStringWidth("Search: " + searchQuery);
                int boxX = 10;
                int boxY = this.height - 35;
                int boxWidth = textWidth + padding * 2;
                int boxHeight = FontUtil.normal.getHeight() + padding * 2;
                
                int boxBgColor = (int) (150 * searchAlpha) << 24 | 0x000000;
                RenderUtils.drawRoundedRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 4, boxBgColor);
                
                FontUtil.normal.drawSmoothString("Search: " + searchQuery, boxX + padding, boxY + padding, searchColor);
            } else {
                clearSearch();
            }
        }
    }

    @Override
	public void mouseClicked(int x, int y, int mouseButton) throws IOException {
        terminal.mouseDown(x, y, mouseButton);
        
        // Check for category drag initiation
        for(CategoryComponent category : visableCategoryList()) {
            // Check if mouse is over category header (for dragging)
            if (isMouseOverCategoryHeader(category, x, y)) {
                if (mouseButton == 0) { // Left click for dragging
                    isDragging = true;
                    draggedCategory = category;
                    dragOffsetX = x - category.getX();
                    dragOffsetY = y - category.getY();
                    return;
                }
            }
            
            // Normal category interaction
            if(category.mouseDown(x, y, mouseButton)) {
                lastCategory = category;
                return;
            }
        }
    }

    @Override
	public void mouseReleased(int x, int y, int mouseButton) {
        terminal.mouseReleased(x, y, mouseButton);
        if (terminal.overPosition(x, y))
            return;

        // Handle drag release
        if (isDragging && draggedCategory != null) {
            isDragging = false;
            draggedCategory = null;
        }

        visableCategoryList().forEach(category -> category.mouseReleased(x, y, mouseButton));

        if (Raven.clientConfig != null)
			Raven.clientConfig.saveConfig();
    }
    
    @Override
	public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel() * 5;
        visableCategoryList().forEach(category -> {
            if(category.isMouseOver(mouseX, mouseY))
                category.scroll(i);
            });
            
        // Handle dragging
        if (isDragging && draggedCategory != null) {
            int newX = mouseX - dragOffsetX;
            int newY = mouseY - dragOffsetY;
            
            // Keep category within screen bounds
            newX = Math.max(0, Math.min(newX, this.width - draggedCategory.getWidth()));
            newY = Math.max(0, Math.min(newY, this.height - draggedCategory.getHeight()));
            
            draggedCategory.setCoords(newX, newY);
        }
    }
    
    private boolean isMouseOverCategoryHeader(CategoryComponent category, int mouseX, int mouseY) {
        return mouseX >= category.getX() && mouseX <= category.getX() + category.getWidth() &&
               mouseY >= category.getY() && mouseY <= category.getY() + 20; // Header area
    }
    
    
    @Override
	public void keyTyped(char t, int k) {
        terminal.keyTyped(t, k);
        if(lastCategory != null)
            lastCategory.keyTyped(t, k);
        if (k == 1) {
            Raven.mc.displayGuiScreen(null);
            Raven.configManager.save();
            Raven.clientConfig.saveConfig();
        }
        
        // Quicksearch functionality
        if (Character.isLetter(t) || Character.isDigit(t)) {
            long currentTime = System.currentTimeMillis();
            
            // Reset search if timeout has passed
            if (currentTime - lastSearchTime > SEARCH_TIMEOUT) {
                searchQuery = "";
            }
            
            // Add character to search query
            searchQuery += Character.toLowerCase(t);
            lastSearchTime = currentTime;
            
            // Perform search
            performModuleSearch(searchQuery);
        } else if (k == 14) { // Backspace key
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                if (searchQuery.isEmpty()) {
                    clearSearch();
                } else {
                    performModuleSearch(searchQuery);
                }
                lastSearchTime = System.currentTimeMillis();
            }
        } else if (k == 28) { // Enter key - clear search
            clearSearch();
        }
    }
    
    private void performModuleSearch(String query) {
        if (query.isEmpty()) {
            clearSearch();
            return;
        }
        
        // Search through all categories and modules
        for (CategoryComponent category : visableCategoryList()) {
            boolean categoryMatch = false;
            
            // Check if category name matches
            if (category.categoryName.name().toLowerCase().contains(query)) {
                categoryMatch = true;
            }
            
            // Highlight matching category
            if (categoryMatch) {
                // You could add visual highlighting here
            }
        }
    }
    
    private void clearSearch() {
        searchQuery = "";
        lastSearchTime = 0;
        // Reset all categories to normal state
        for (CategoryComponent category : visableCategoryList()) {
            // Reset any search-specific visual states
        }
    }

    
    @Override
	public void onGuiClosed() {
        visableCategoryList().forEach(CategoryComponent::guiClosed);
        Raven.configManager.save();
        Raven.clientConfig.saveConfig();
    }

    @Override
	public boolean doesGuiPauseGame() {
        return false;
    }

    public ArrayList<CategoryComponent> getCategoryList() {
        return categoryList;
    }

    public CategoryComponent getCategoryComponent(ModuleCategory mCat) {
        for (CategoryComponent cc : categoryList)
			if (cc.categoryName == mCat)
                return cc;
        return null;
    }

    public ArrayList<CategoryComponent> visableCategoryList() {
        ArrayList<CategoryComponent> newList = (ArrayList<CategoryComponent>) categoryList.clone();
        newList.removeIf(obj -> !obj.visable);
        return newList;
    }

    public void resetSort() {
        int xOffSet = 5;
        int yOffSet = 5;
        for(CategoryComponent category : categoryList) {
            category.setCoords(xOffSet, yOffSet);
            xOffSet = xOffSet + 100;
            if (xOffSet > 400) {
                xOffSet = 5;
                yOffSet += 120;
            }
        }

    }
}
