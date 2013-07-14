/**
 * Highlight zone updates in different colors
 */

Tapestry.ElementEffect.colorhighlight = function(element, color) 
{
    if (color)
        return new Effect.Highlight(element, {
            startcolor : '%s',
            endcolor: color,
            restorecolor: color
        });
    return new Effect.Highlight(element, { 
        startcolor : '%s',
        restorecolor: $(element).savedBackgroundColor
        }
    );
};
